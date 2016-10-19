package dcomposer

trait UiLauncher {
  def uiPort = 8089

  def launchUi(): Unit = {
    val server = new JettyServer(uiPort, this.getClass.getClassLoader.getResource(".").toExternalForm) {
      override def route(method: String, path: String, param: String => String) = (method, path) match {
        case ("GET", "/") =>
          val query = param("q")
          Some(s"""{"a":"Hi $query"}""")
        case _ =>
          None
      }
    }
    server.start()
    java.awt.Desktop.getDesktop.browse(new java.net.URI(s"http://localhost:$uiPort/"))

    println("Press Enter to stop UI server.")
    while (io.Source.stdin.getLines().next().nonEmpty) {}
    server.stop()
    println("Stopped.")
  }
}
