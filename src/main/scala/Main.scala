import utils.ExternalJsUtil

import scala.scalajs.js

object Main {

  @SuppressWarnings(scala.Array("org.wartremover.warts.StringPlusAny"))
  def main(args: Array[String]): Unit = {
    println(getClass.getName + s": Hello console world at ${new js.Date}!")
    new ExternalJsUtil().testMethod()
  }
}
