package dcomposer

import org.eclipse.jetty.server.Server

import scala.annotation.tailrec

object Main extends App {

  println("Usage:")
  println("  - type :web to open web ui")
  println("  - type an artifact name pattern")
  println("  - type :number (e.g. :1) to choose from search result")
  println("  - type :number again to choose version from search result, :0 is the latest release")
  println("  - type :[sbt|cbt|mvn|gradle] (e.g. :sbt) to generate sbt, maven or gradle dependency list from chosen results")
  println("  - type :x to exit")

  val port = 8089
  val resultNumberMask = ":(\\d+)".r
  var server: Option[Server] = None

  @tailrec
  def processInput(state: State): Unit = {
    io.Source.stdin.getLines().next() match {
      case ":x" =>
        server.foreach(_.stop())
      case ":web" =>
        java.awt.Desktop.getDesktop.browse(new java.net.URI(s"http://localhost:$port/"))
        server = launchWebUi()
      case ":sbt" =>
        println(state.generateSbt)
        processInput(state)
      case ":cbt" =>
        println(state.generateCbt)
        processInput(state)
      case ":mvn" =>
        println(state.generateMvn)
        processInput(state)
      case ":gradle" =>
        println(state.generateGradle)
        processInput(state)
      case resultNumberMask(n) =>
        val number = Integer.valueOf(n)
        processInput(state.enterNumber(number))
      case pattern =>
        processInput(state.search(pattern))
    }
  }

  def launchWebUi() = {
    import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

    import org.eclipse.jetty.server.Request
    import org.eclipse.jetty.server.handler._

    val server = new Server(port)

    val c0 = new ContextHandler("/")
    val resourceHandler = new ResourceHandler
    resourceHandler.setDirectoriesListed(true)
    val base = this.getClass.getClassLoader.getResource(".").toExternalForm
    resourceHandler.setResourceBase(base)
    resourceHandler.setWelcomeFiles(Array("index.html"))
    c0.setHandler(resourceHandler)

    val c1 = new ContextHandler("/api")
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
    c1.setHandler(handler1)

    val handlers = new HandlerCollection
    handlers.setHandlers(Array(c0, c1, new DefaultHandler))
    server.setHandler(handlers)

    server.start()
    server.dumpStdErr()
    server.join()

    Some(server)
  }

  val ds = new MavenCentralDs

  processInput(SearchArtifact(ds))
}
