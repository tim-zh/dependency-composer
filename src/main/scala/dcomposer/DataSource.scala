package dcomposer

import java.net.SocketTimeoutException

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper

import scalaj.http.Http

trait DataSource {

  def searchDependency(query: String): IndexedSeq[Dependency]

  def searchVersion(dependency: Dependency, useVersionMask: Boolean = false): IndexedSeq[String]

  def fullScalaVersion(major: String): String

  protected def prependLatestRelease(versions: IndexedSeq[String]) = {
    val releases = versions.filter(_.toLowerCase.matches("^[\\d.(final)v]+$"))
    if (releases.isEmpty)
      versions
    else {
      val latest = releases.max(Ordering.fromLessThan[String]((a, b) => a.compareTo(b) < 0))
      val (head, tail) = versions.partition(_ == latest)
      head ++ tail
    }
  }
}

class MavenCentralDs extends DataSource {
  val jsonListOfDocs = {
    import scala.collection.JavaConverters._
    val mapper = new ObjectMapper
    (json: String) => mapper.readTree(json).path("response").withArray("docs").elements().asScala.toIndexedSeq
  }

  override def searchDependency(query: String) = {
    val request = Http("http://search.maven.org/solrsearch/select")
        .param("q", query)
        .param("rows", "30")
        .param("wt", "json")
    try {
      val response = request.asString
      jsonListOfDocs(response.body).map { obj =>
        val group = obj.get("g").asText()
        val artifact = obj.get("a").asText()
        val version = obj.get("latestVersion").asText()
        Dependency(group, artifact, version)
      }
    } catch {
      case e: JsonParseException =>
        println(s"  incorrect input from backend: ${e.getOriginalMessage}")
        IndexedSeq()
      case _: SocketTimeoutException =>
        println("  connection timeout")
        IndexedSeq()
    }
  }

  override def searchVersion(dependency: Dependency, useVersionMask: Boolean) = {
    val postfix = if (useVersionMask)
      s"+AND+v:${dependency.version}*"
    else
      ""
    val decodedQuery = s"""q=g:"${dependency.group}"+AND+a:"${dependency.artifact}"$postfix"""
    val request = Http("http://search.maven.org/solrsearch/select?" + decodedQuery)
        .param("rows", "30")
        .param("wt", "json")
        .param("core", "gav")
    try {
      val response = request.asString
      val versions = jsonListOfDocs(response.body).map { obj =>
        obj.get("v").asText()
      }
      prependLatestRelease(versions)
    } catch {
      case e: JsonParseException =>
        println(s"  incorrect input from backend: ${e.getOriginalMessage}")
        IndexedSeq()
      case _: SocketTimeoutException =>
        println("  connection timeout")
        IndexedSeq()
    }
  }

  override def fullScalaVersion(major: String) = {
    val versions = searchVersion(Dependency("org.scala-lang", "scala-compiler", major), useVersionMask = true).filter(_.matches("^[\\d.]+$"))
    if (versions.isEmpty)
      major + "._"
    else
      versions.max(Ordering.fromLessThan[String]((a, b) => a.compareTo(b) < 0))
  }
}
