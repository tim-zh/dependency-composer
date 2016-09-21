package dcomposer

import scala.annotation.tailrec

object Main extends App {
  println("Usage:")
  println("  - type an artifact name pattern")
  println("  - type :number (e.g. :1) to choose from search result")
  println("  - type :number again to choose version from search result, :0 is the latest release")
  println("  - type :go to generate sbt libraryDependencies from chosen results")
  println("  - type :[central|bintray] (default is :central) to choose search backend")
  println("  - type :x to exit")

  @tailrec
  def processInput(state: State): Unit = {
    val resultNumberMask = ":(\\d+)".r
    io.Source.stdin.getLines().next() match {
      case ":x" =>
        return
      case ":bintray" =>
        processInput(state.setDs(new BinTrayDs))
      case ":central" =>
        processInput(state.setDs(new MavenCentralDs))
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

  val ds = new MavenCentralDs

  processInput(SearchArtifact(ds))
}
