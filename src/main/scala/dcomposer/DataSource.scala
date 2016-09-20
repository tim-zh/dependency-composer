package dcomposer

import play.api.libs.json.{JsArray, JsObject, Json}

import scalaj.http.Http


trait DataSource {
  def searchDependency(query: String): IndexedSeq[Dependency]

  def searchVersion(dependency: Dependency): IndexedSeq[String]

  protected def findLatestRelease(versions: Seq[String]) = {
    val version = "^\"[\\d.]+\"$".r
    versions.filter(version.findFirstIn(_).isDefined).reduceOption((a, b) => if (a.compareTo(b) < 0) b else a)
  }

  protected def prependLatestRelease(versions: IndexedSeq[String]) = {
    val version = "^\"[\\d.]+\"$".r
    if (versions.isEmpty)
      versions
    else {
      val latest = versions.filter(version.findFirstIn(_).isDefined).max(Ordering.fromLessThan[String]((a, b) => a.compareTo(b) < 0))
      latest +: versions
    }
  }
}

trait MavenCentralDS extends DataSource {
  override def searchDependency(query: String) = {
    val request = Http("http://search.maven.org/solrsearch/select")
        .param("q", query)
        .param("rows", "20")
        .param("wt", "json")
    val response = request.asString
    (Json.parse(response.body) \ "response" \ "docs").as[JsArray].value.toIndexedSeq.map(_.as[JsObject]).map { obj =>
      val group = obj \ "g" toString()
      val artifact = obj \ "a" toString()
      val version = obj \ "latestVersion" toString()
      Dependency(group, artifact, version)
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
      obj \ "v" toString()
    }
    prependLatestRelease(versions)
  }
}
