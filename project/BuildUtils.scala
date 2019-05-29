import sbt._
import sbt.internal.util.ManagedLogger

import scala.sys.process.Process

case class ResourceKeys(staticDir: File,
                        templatesDir: File,
                        providedJsDir: File,
                        productName: String,
                        version: String,
                        isRelease: Boolean)

case class ElectronKeys(
  packageName: String, // can't contain any non-URL-safe characters, all lowercase
  packageDescription: String,
  authorName: String,
  authorEmail: String,
  authorUrl: String,
  appId: String, // CFBundleIdentifier (mac) or Application User Model ID (win)
  updateCheckUrl: String // ${os} placeholder is expanded to mac, linux or win
)

object BuildUtils {

  def validatePlatform(name: Option[String]): Platform =
    name.map(_.trim.toLowerCase) match {
      case Some("mac") => Platform.Mac
      case Some("win") => Platform.Win
      case Some("linux") => Platform.Linux
      case other => Platform.Unknown(other.getOrElse(""))
    }

  def collectResources(targetDir: File, keys: ResourceKeys, electron: ElectronKeys): Unit = {
    copyDirContent(keys.staticDir, targetDir)
    copyDirContent(keys.providedJsDir, targetDir)

    val replacements = Map(
      "%version%" -> keys.version,
      "%isRelease%" -> keys.isRelease.toString,
      "%productName%" -> keys.productName,
      //
      "%packageName%" -> electron.packageName,
      "%packageDescription%" -> electron.packageDescription,
      "%authorName%" -> electron.authorName,
      "%authorEmail%" -> electron.authorEmail,
      "%authorUrl%" -> electron.authorUrl,
      "%appId%" -> electron.appId,
      "%updateCheckUrl%" -> electron.updateCheckUrl
    )

    println(s"Collecting template files from ${keys.templatesDir.getPath}")

    val templates = (keys.templatesDir * "*").filter(_.isFile).get
    templates.foreach { t =>
      copyFile(t, targetDir, replacements)
    }

    println(s"Resources were copied to ${targetDir.getPath}")
  }

  def copyDirContent(sourceDir: File, targetDir: File): Unit = {
    println(s"Collecting static files from ${sourceDir.getPath}")

    val dirs = (sourceDir * "*").filter(_.isDirectory).get
    val files = (sourceDir * "*").filter(_.isFile).get
    dirs.foreach { d =>
      println(s"\tCopying /${d.getName}/")
      IO.copyDirectory(d, targetDir / d.getName, overwrite = true)
    }
    files.foreach { f =>
      println(s"\tCopying /${f.getName}")
      IO.copyFile(f, targetDir / f.getName)
    }
  }

  def copyFile(sourceFile: File, targetFolder: File, replacements: Map[String, String]): Unit = {
    println(s"\tCopying ${sourceFile.getName} with ${replacements.size} replacements.")
    val source = IO.read(sourceFile, IO.utf8)
    val content = replacements.foldLeft(source) { (acc, next) =>
      acc.replaceAllLiterally(next._1, next._2)
    }
    IO.write(targetFolder / sourceFile.getName, content, IO.utf8)
  }

  def startElectronBuild(param: Option[String],
                         webBuild: File,
                         targetFolder: File,
                         log: ManagedLogger): Unit =
    validatePlatform(param) match {
      case Platform.Unknown(input) =>
        log.error(s"Unknown target platform: '$input'.")
        log.error(
          s"Use ${Platform.Mac.name}, ${Platform.Win.name} or ${Platform.Linux.name} as task parameter."
        )
      case platform =>
        IO.delete(targetFolder / "app")
        IO.copyDirectory(webBuild, targetFolder / "app")
        IO.move(
          targetFolder / "app" / "electron_builder_package.json",
          targetFolder / "package.json"
        )
        IO.move(
          targetFolder / "app" / "electron_package.json",
          targetFolder / "app" / "package.json"
        )
        IO.move(targetFolder / "app" / "electron_main.js", targetFolder / "app" / "main.js")

        val deps = Process("yarn install", targetFolder).!
        require(deps == 0, s"yarn install failed with code $deps")

        val app = Process(s"yarn run ${platform.name}-dist", targetFolder).!
        require(app == 0, s"Electron build failed with code $app")

        println(s"\nElectron build result:\n${(targetFolder / "dist").getPath}\n")
    }
}
