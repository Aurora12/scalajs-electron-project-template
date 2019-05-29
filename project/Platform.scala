sealed trait Platform {
  def name: String
}

object Platform {
  case object Mac extends Platform {
    val name = "mac"
  }
  case object Win extends Platform {
    val name = "win"
  }
  case object Linux extends Platform {
    val name = "linux"
  }
  case class Unknown(name: String) extends Platform
}
