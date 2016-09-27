package dcomposer

import scala.annotation.tailrec

object Main extends App {

  println("Usage:")
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

  val ds = new MavenCentralDs

  processInput(SearchArtifact(ds))
}
