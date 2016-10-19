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
    handlers.setHandlers(Array(handlerStatic, handlerApi, new DefaultHandler))
    server.setHandler(handlers)

    private def handlerStatic = {
      val handler = new ContextHandler("/")
      val resourceHandler = new ResourceHandler
      resourceHandler.setDirectoriesListed(true)
      val base = this.getClass.getClassLoader.getResource(".").toExternalForm
      resourceHandler.setResourceBase(base)
      resourceHandler.setWelcomeFiles(Array("index.html"))
      handler.setHandler(resourceHandler)
      handler
    }

    private def handlerApi = {
      import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

      val handler = new ContextHandler("/api")
      val handler1 = new AbstractHandler {
        override def handle(target: String,
                            baseRequest: Request,
                            request: HttpServletRequest,
                            response: HttpServletResponse) = {
          response.setContentType("text/html; charset=utf-8")
          response.setStatus(HttpServletResponse.SC_OK)
          val out = response.getWriter
          out.println("<h1>Hi</h1>")
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
