package dcomposer

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.server.{Request, Server}
import org.eclipse.jetty.server.handler._

abstract class JettyServer(port: Int, staticFilesUrl: String) {
  private val server = {
    val s = new Server(port)
    val handlers = new HandlerCollection
    handlers.setHandlers(Array(handlerOfStatic, handlerOfApi, new DefaultHandler))
    s.setHandler(handlers)
    s
  }

  def start() = server.start()

  def stop() = server.stop()

  def route(method: String, path: String, param: String => String): Option[String]

  private def handlerOfStatic = {
    val handler = new ContextHandler("/")
    val resourceHandler = new ResourceHandler
    resourceHandler.setDirectoriesListed(true)
    resourceHandler.setResourceBase(staticFilesUrl)
    handler.setHandler(resourceHandler)
    handler
  }

  private def handlerOfApi = {
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
}
