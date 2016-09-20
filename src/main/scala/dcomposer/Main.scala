package dcomposer

import scala.annotation.tailrec

object Main extends App {
  println("Usage:")
  println("  - type an artifact name pattern")
  println("  - type :number (e.g. :1) to choose from search result")
  println("  - type :number again to choose version from search result, :0 is the latest release")
  println("  - type :go to generate sbt libraryDependencies from chosen results")
  println("  - type :x to exit")

  val ds = new MavenCentralDs

  @tailrec
  def processInput(state: State): Unit = {
    val resultNumberMask = ":(\\d+)".r
    io.Source.stdin.getLines().next() match {
      case ":x" =>
        return
      case ":sbt" =>
        println(state.generateSbt)
        processInput(state)
      case resultNumberMask(n) =>
        val number = Integer.valueOf(n)
        processInput(state.enterNumber(number))
      case pattern =>
        processInput(state.search(pattern))
    }
  }

  processInput(SearchArtifact(ds))
}
