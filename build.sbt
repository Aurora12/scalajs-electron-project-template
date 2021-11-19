import complete.DefaultParsers._
import sbt.Keys.artifactPath

name := "scalajs-electron-project-template"
version := "0.0.1"
scalaVersion := "2.13.7"

scalaJSUseMainModuleInitializer := true
scalafmtOnCompile := true

ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "utf-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint:-unused,_",
  "-Xcheckinit",
  "-Xfatal-warnings",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused:imports"
)

lazy val wartremoverSettings = Seq(
  Compile / compile / wartremoverErrors ++= Seq(
    Wart.EitherProjectionPartial,
    Wart.IsInstanceOf,
    Wart.TraversableOps,
    Wart.Null,
    Wart.OptionPartial,
    Wart.Return,
    Wart.StringPlusAny,
    Wart.TryPartial
  )
)

lazy val targetDirectory = settingKey[File]("Target build directory")
lazy val webBuildDir = settingKey[File]("Web build directory")
lazy val electronBuildDir = settingKey[File]("Electron build directory")

lazy val buildVersion = settingKey[String]("Build version from -Dversion parameter.")

lazy val webRelease = inputKey[Unit]("Build release web application")
lazy val webDev = inputKey[Unit]("Build dev web application")
lazy val electron =
  inputKey[Unit]("Build electron app for selected platform without rebuilding web")

targetDirectory := baseDirectory.value / "bin"
webBuildDir := targetDirectory.value / "web"
electronBuildDir := targetDirectory.value / "electron"

Compile / fastOptJS / artifactPath := webBuildDir.value / "js" / "main.js"
Compile / fullOptJS / artifactPath := webBuildDir.value / "js" / "main.js"
Compile / packageJSDependencies / artifactPath := webBuildDir.value / "js" / "dependencies.js"
clean ~= { _ =>
  IO.delete(new File("./bin"))
}
Compile / fastOptJS ~= { result =>
  println(s"\nfastOptJS result:\n${result.data.getPath}\n")
  result
}
Compile / fullOptJS ~= { result =>
  println(s"\nfullOptJS result:\n${result.data.getPath}\n")
  result
}

lazy val project =
  Project("ScalajsElectronProjectTemplate", file("."))
    .enablePlugins(ScalaJSPlugin, JSDependenciesPlugin)
    .settings(
      buildVersion := sys.props.get("version").getOrElse(version.value),
      wartremoverSettings,
      targetDirectory := baseDirectory.value / "bin",
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "1.2.0",
        "org.querki" %%% "jquery-facade" % "2.0"
      ),
      jsDependencies ++= Seq(
        "org.webjars" % "jquery" % "3.6.0" / "jquery.js" minified "jquery.min.js",
        ProvidedJS / "js/example.js"
      ),
      webRelease := {
        val log = streams.value.log
        log.info(s"Starting RELEASE build")

        val keys = ResourceKeys(
          baseDirectory.value / "resources" / "static",
          baseDirectory.value / "resources" / "templates",
          baseDirectory.value / "src" / "main" / "resources",
          "Scala.js Project Template With Electron",
          buildVersion.value,
          isRelease = true
        )

        val electronKeys = ElectronKeys(
          "scalajs-electron",
          "Scala.js Project Template With Electron",
          "Aurora12",
          "noregret.org@gmail.com",
          "https://github.com/Aurora12",
          "org.noregret.scalajs.electron",
          "https://example.com/update/${os}"
        )

        BuildUtils.collectResources(webBuildDir.value, keys, electronKeys)
      },
      webRelease := webRelease.dependsOn(Compile / compile, Compile / fullOptJS).evaluated,
      webDev := {
        val log = streams.value.log
        log.info(s"Starting DEV build")

        val keys = ResourceKeys(
          baseDirectory.value / "resources" / "static",
          baseDirectory.value / "resources" / "templates",
          baseDirectory.value / "src" / "main" / "resources",
          "Scala.js Project Template (dev version)",
          buildVersion.value,
          isRelease = false
        )

        val electronKeys = ElectronKeys(
          "scalajs-electron-dev",
          "Scala.js Project Template With Electron (dev version)",
          "Aurora12",
          "noregret.org@gmail.com",
          "https://github.com/Aurora12",
          "org.noregret.scalajs.electron.dev",
          "https://dev.example.com/update/${os}"
        )

        BuildUtils.collectResources(webBuildDir.value, keys, electronKeys)
      },
      webDev := webDev.dependsOn(Compile / compile, Compile / fastOptJS).evaluated,
      electron := {
        val log = streams.value.log
        log.info(s"Starting electron build")
        BuildUtils
          .startElectronBuild(
            spaceDelimited("<arg>").parsed.headOption,
            webBuildDir.value,
            electronBuildDir.value,
            log
          )
      }
    )

// shortcuts for clean & build

commands += Command.command("electronMac") { s =>
  "clean" ::
    "webRelease" ::
    "electron mac" ::
    s
}

commands += Command.command("electronWin") { s =>
  "clean" ::
    "webRelease" ::
    "electron win" ::
    s
}

commands += Command.command("electronLinux") { s =>
  "clean" ::
    "webRelease" ::
    "electron linux" ::
    s
}

commands += Command.command("release") { s =>
  "clean" ::
    "webRelease" ::
    s
}

commands += Command.command("dev") { s =>
  "clean" ::
    "webDev" ::
    s
}
