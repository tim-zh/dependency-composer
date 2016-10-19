package dcomposer

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler._

trait UiLauncher {
  def uiPort = 8089

  def launchUi(): Unit = {
    val server = new JettyServer(uiPort)
    server.start()
    java.awt.Desktop.getDesktop.browse(new java.net.URI(s"http://localhost:$uiPort/"))

    println("Press Enter to stop UI server.")
    while (io.Source.stdin.getLines().next().nonEmpty) {}
    server.stop()
    println("Stopped.")
  }

  private class JettyServer(port: Int) {
    val server = new Server(uiPort)

    val handlers = new HandlerCollection
    handlers.setHandlers(Array(handlerOfStatic, handlerOfApi, new DefaultHandler))
    server.setHandler(handlers)

    def route(method: String, path: String, param: String => String) = (method, path) match {
      case ("GET", "/") =>
        val query = param("q")
        Some(s"""{"a":"Hi $query"}""")
      case _ =>
        None
    }

    def handlerOfStatic = {
      val handler = new ContextHandler("/")
      val resourceHandler = new ResourceHandler
      resourceHandler.setDirectoriesListed(true)
      val base = this.getClass.getClassLoader.getResource(".").toExternalForm
      resourceHandler.setResourceBase(base)
      resourceHandler.setWelcomeFiles(Array("index.html"))
      handler.setHandler(resourceHandler)
      handler
    }

    def handlerOfApi = {
      import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

      val handler = new ContextHandler("/api/")
      val handler1 = new AbstractHandler {
        override def handle(target: String,
                            baseRequest: Request,
                            request: HttpServletRequest,
                            response: HttpServletResponse) = {
          response.setContentType("application/json")
          response.setCharacterEncoding("UTF-8")

          route(request.getMethod, target, request.getParameter).fold {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
          } {
            response.getWriter.write _
          }

          baseRequest.setHandled(true)
        }
      }
      handler.setHandler(handler1)
      handler
    }

    def start() = server.start()

    def stop() = server.stop()
  }
}
