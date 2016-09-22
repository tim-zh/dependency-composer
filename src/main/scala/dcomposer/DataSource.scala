package dcomposer

import java.net.SocketTimeoutException

import play.api.libs.json.{JsArray, JsObject, Json}

import scalaj.http.Http


trait DataSource {
  def searchDependency(query: String): IndexedSeq[Dependency]

  def searchVersion(dependency: Dependency): IndexedSeq[String]

  protected def prependLatestRelease(versions: IndexedSeq[String]) = {
    val version = "^[\\d.]+$".r
    val releases = versions.filter(version.findFirstIn(_).isDefined)
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
  override def searchDependency(query: String) = {
    val request = Http("http://search.maven.org/solrsearch/select")
        .param("q", query)
        .param("rows", "20")
        .param("wt", "json")
    try {
      val response = request.asString
      (Json.parse(response.body) \ "response" \ "docs").as[JsArray].value.toIndexedSeq.map(_.as[JsObject]).map { obj =>
        val group = (obj \ "g").as[String]
        val artifact = (obj \ "a").as[String]
        val version = (obj \ "latestVersion").as[String]
        Dependency(group, artifact, version)
      }
    } catch {
      case _: SocketTimeoutException =>
        println("  connection timeout")
        IndexedSeq()
    }
  }

  override def searchVersion(dependency: Dependency) = {
    val decodedQuery = s"""q=g:${dependency.group}+AND+a:${dependency.artifact}"""
    val request = Http("http://search.maven.org/solrsearch/select?" + decodedQuery)
        .param("rows", "20")
        .param("wt", "json")
        .param("core", "gav")
    val response = request.asString
    val versions = (Json.parse(response.body) \ "response" \ "docs").as[JsArray].value.toIndexedSeq.map(_.as[JsObject]).map { obj =>
      (obj \ "v").as[String]
    }
    prependLatestRelease(versions)
  }
}

class BinTrayDs extends DataSource {
  private var versionCache = Map[Dependency, IndexedSeq[String]]()

  override def searchDependency(query: String) = {
    val request = Http("https://api.bintray.com/search/packages/maven")
        .param("q", s"*$query*")
    try {
      val response = request.asString
      val result = Json.parse(response.body).as[JsArray].value.toIndexedSeq.map(_.as[JsObject]).flatMap { obj =>
        val version = (obj \ "latest_version").as[String]
        val versions = (obj \ "versions").as[JsArray].value.toIndexedSeq.map(_.as[String])
        (obj \ "system_ids").as[JsArray].value.toIndexedSeq.map(_.as[String]).map { name =>
          val group = name.substring(0, name.indexOf(":"))
          val artifact = name.substring(name.indexOf(":") + 1)
          (Dependency(group, artifact, version), versions)
        }
      }
      result.foreach { case (dependency, versions) => versionCache += dependency -> versions }
      result.map(_._1)
    } catch {
      case _: SocketTimeoutException =>
        println("  connection timeout")
        IndexedSeq()
    }
  }

  override def searchVersion(dependency: Dependency) = {
    prependLatestRelease(versionCache(dependency))
  }
}
