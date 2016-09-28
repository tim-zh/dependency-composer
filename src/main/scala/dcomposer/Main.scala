package dcomposer

import scala.annotation.tailrec

object Main extends App {

  println("Usage:")
  println("  - type :web to open web ui")
  println("  - type an artifact name pattern")
  println("  - type :number (e.g. :1) to choose from search result")
  println("  - type :number again to choose version from search result, :0 is the latest release")
  println("  - type :[sbt|mvn|gradle] (e.g. :sbt) to generate sbt, maven or gradle dependency list from chosen results")
  println("  - type :x to exit")

  val resultNumberMask = ":(\\d+)".r

  @tailrec
  def processInput(state: State): Unit = {
    io.Source.stdin.getLines().next() match {
      case ":x" =>
      case ":web" =>
        launchWebUi()
      case ":sbt" =>
        println(state.generateSbt)
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
    import org.http4s._
    import org.http4s.dsl._
    import org.http4s.server.blaze._
    import org.http4s.server.syntax._
    import scalaz.concurrent.Task

    val helloWorldService = HttpService {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")
    }
    val helloWorldService2 = HttpService {
      case GET -> Root / "hello2" / name =>
        StaticFile.fromFile(new java.io.File("readme.md")).fold(NotFound())(Task.now)
    }
    val services = helloWorldService orElse helloWorldService2
    val builder = BlazeBuilder.bindHttp(8080, "localhost").mountService(helloWorldService, "/").mountService(helloWorldService2, "/2")
    val server = builder.run

    java.awt.Desktop.getDesktop.browse(new java.net.URI("http://localhost:8080/"))
    Thread.sleep(10000)
  }

  {
    import java.io.IOException
    import java.io.OutputStream
    import java.net.InetSocketAddress
    import com.sun.net.httpserver.HttpExchange
    import com.sun.net.httpserver.HttpHandler
    import com.sun.net.httpserver.HttpServer

    val server = HttpServer.create(new InetSocketAddress(8080), 0)
    server.createContext("/test", new Test.MyHandler)
    server.setExecutor(null) // creates a default executor
    server.start()

    private[dcomposer] class MyHandler extends HttpHandler {
      @throws[IOException]
      def handle(t: HttpExchange) {
        val response: String = "This is the response"
        t.sendResponseHeaders(200, response.length)
        val os: OutputStream = t.getResponseBody
        os.write(response.getBytes)
        os.close()
      }
    }
  }

  val ds = new MavenCentralDs

  processInput(SearchArtifact(ds))
}
