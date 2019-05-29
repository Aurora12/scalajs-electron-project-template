package utils

import org.scalajs.dom.experimental.{Notification, NotificationOptions}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.{document, window}

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global

@SuppressWarnings(scala.Array("org.wartremover.warts.Null"))
object ElectronHelper {

  private object Events {
    val updateBadgeIcon = "updateBadgeIcon"
    val info = "info"
    val warn = "warn"
    val error = "error"
    val doNotify = "notify"
    val requestUpdate = "requestUpdate"
    val acceptUpdate = "acceptUpdate"
    val updateProgress = "updateProgress"
    val checkForUpdate = "checkForUpdate"
    val askToQuit = "askToQuit"
    val readyToQuit = "readyToQuit"
    val confirmQuit = "confirmQuit"
  }

  lazy val isElectron = {
    val process = window.asInstanceOf[js.Dynamic].process
    if (js.isUndefined(process)) false
    else {
      val versions = process.versions
      !js.isUndefined(versions) && !js.isUndefined(versions.electron)
    }
  }

  private lazy val electron = global.window.nodeRequire("electron")
  private lazy val ipcRenderer = electron.ipcRenderer
  private lazy val app = electron.remote.app
  lazy val platform = electron.remote.process.platform.toString
  lazy val version =
    s"${electron.remote.process.versions.electron}_${electron.remote.process.versions.chrome}"
  private lazy val isWindows = platform.startsWith("win")

  def checkForUpdate = ipcRenderer.send(Events.checkForUpdate)

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def showBadgeCount(count: Int): Unit =
    if (isWindows)
      if (count > 0) {
        val text = if (count > 99) "99+" else if (count > 0) count.toString else ""
        ipcRenderer.send(Events.updateBadgeIcon, drawCountIcon(text))
      } else
        ipcRenderer.send(Events.updateBadgeIcon, null)
    else
      app.setBadgeCount(count)

  private def drawCountIcon(text: String) = {
    val canvas = document.createElement("canvas").asInstanceOf[Canvas]
    canvas.width = 140
    canvas.height = 140

    val ctx = canvas.getContext("2d")
    ctx.fillStyle = "#F54458"
    ctx.beginPath()
    ctx.ellipse(70, 70, 70, 70, 0, 0, 2 * Math.PI)
    ctx.fill()

    ctx.textAlign = "center"
    ctx.fillStyle = "white"

    if (text.length > 1) {
      ctx.font = "100px sans-serif"
      ctx.fillText(text, 70, 105)
    } else {
      ctx.font = "125px sans-serif"
      ctx.fillText(text, 70, 112)
    }

    canvas.toDataURL("PNG")
  }

  def initialize(): Unit = {
    ipcRenderer.on(
      Events.info,
      (_: js.Object, message: js.Object) => println(s"[Info] ELECTRON: $message")
    )
    ipcRenderer.on(
      Events.warn,
      (_: js.Object, message: js.Object) => println(s"[Warn] ELECTRON: $message")
    )
    ipcRenderer.on(
      Events.error,
      (_: js.Object, message: js.Object) => println(s"[Error] ELECTRON: $message")
    )
    ipcRenderer.on(
      Events.doNotify,
      (_: js.Object, title: js.Object, message: js.Object) =>
        new Notification(
          title.toString,
          NotificationOptions(body = message.toString, sticky = true, silent = true)
      )
    )
    ipcRenderer.on(Events.requestUpdate, (_: js.Object, version: js.Object) => {
      println(s"ELECTRON: requesting update to $version. Ask user to accept or deny.")
      // when ready:
      // ipcRenderer.send(Events.acceptUpdate)
    })
    ipcRenderer.on(Events.updateProgress, (_: js.Object, percentage: js.Object) => {
      println(s"ELECTRON: update progress $percentage")
    })
    ipcRenderer.on(Events.confirmQuit, (_: js.Object) => {
      window.onbeforeunload = null
      println(s"ELECTRON: confirmed quit!")
      //
      // do something to finalize work before quit
      // when ready, send the following:
      ipcRenderer.send(Events.readyToQuit)
    })
    println("Renderer process handlers set up!")
  }

  def askToQuit(question: String) =
    ipcRenderer.send(Events.askToQuit, question, "Quit", "Stay")

}
