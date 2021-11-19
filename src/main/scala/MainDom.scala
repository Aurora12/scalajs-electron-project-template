import org.querki.jquery._
import org.scalajs.dom.{document, _}
import utils.ElectronHelper

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.util.Random

@JSExportTopLevel("MainDom")
object MainDom {

  case class BuildInfo(productName: String, isRelease: Boolean, version: String)

  private var _buildInfo = BuildInfo(getClass.getName, isRelease = false, "0.0.0")
  def buildInfo: BuildInfo = _buildInfo

  def getString(source: js.Dynamic, name: String): Option[String] = {
    val value = source.selectDynamic(name)
    if (js.isUndefined(value) || value == null) None else Option(value.toString)
  }

  @JSExport
  def start(info: js.Dynamic): Unit = {
    println(getClass.getName + ": Hello DOM world!")

    _buildInfo = BuildInfo(
      productName = getString(info, "productName").getOrElse(_buildInfo.productName),
      isRelease = getString(info, "isRelease").fold(false)(_ == "true"),
      version = getString(info, "version").getOrElse(_buildInfo.version)
    )

    useDom()
    useJquery()

    if (ElectronHelper.isElectron) {
      window.onbeforeunload = { e =>
        val quitQuestion = "Are you sure you want to quit?"
        e.asInstanceOf[js.Dynamic].returnValue = quitQuestion
        ElectronHelper.askToQuit(quitQuestion)
        quitQuestion
      }
      ElectronHelper.initialize()
      ElectronHelper.checkForUpdate
    }
  }

  @SuppressWarnings(scala.Array("org.wartremover.warts.StringPlusAny"))
  def useDom(): Unit = {
    val hello = document.createElement("div")
    hello.appendChild(document.createTextNode(s"Hello world with DOM at ${new js.Date}!"))
    document.body.appendChild(hello)

    val info = document.createElement("pre")
    info.appendChild(document.createTextNode(buildInfo.toString))
    document.body.appendChild(info)
  }

  private val rnd: Random = new Random

  def useJquery(): Unit = {
    $("body").append("<div>Hello world with jQuery!</div>")
    $("body").append("<div id='clickTest'>Click me!</div>")
    $("body").append("<div>See console log for more info.</div>")
    if (ElectronHelper.isElectron) {
      $("body").append(
        "<button id='badgeTest'>Set application badge count to random number</button>"
      )
      $("#badgeTest").click((_: JQueryEventObject) =>
        ElectronHelper.showBadgeCount(rnd.nextInt(100))
      )
      $("body").append("<button id='removeBadge'>Remove application badge count</button>")
      $("#removeBadge").click((_: JQueryEventObject) => ElectronHelper.showBadgeCount(0))
    }
    $("#clickTest").click(onClick _)
  }

  def onClick(e: JQueryEventObject): Unit =
    $("body").append("<div>Clicked!</div>")
}
