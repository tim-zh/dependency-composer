package dcomposer

import play.api.libs.json.{JsArray, JsObject, Json}

import scalaj.http.Http

sealed trait State {
  def search(query: String): State

  def enterNumber(n: Int): State

  def generate: String = {
    val versions = cache.flatMap(_.scalaVersion).toSet
    val isDifferentScalaVersions = versions.size > 1
    //scalaVersion := "2.11.8"
    val prefix = if (isDifferentScalaVersions)
      ""
    else
      s"scalaVersion := ${versions.head}\n\n"
    val dependencies = cache.map { d =>
      "\n  " + (if (isDifferentScalaVersions) d.toSbtWithVersion else d.toSbt)
    }.mkString(",")
    s"${prefix}libraryDependencies ++= Seq($dependencies)"
  }

  val cache: Seq[Dependency]
}

case class SearchArtifact(override val cache: Seq[Dependency] = Seq()) extends State {
  override def search(query: String) = {
    val request = Http("http://search.maven.org/solrsearch/select")
        .param("q", query)
        .param("rows", "20")
        .param("wt", "json")
    val response = request.asString
    val dependencies = (Json.parse(response.body) \ "response" \ "docs").as[JsArray].value.toIndexedSeq.map(_.as[JsObject]).map { obj =>
      val group = obj \ "g" toString()
      val artifact = obj \ "a" toString()
      val version = obj \ "latestVersion" toString()
      Dependency(group, artifact, version)
    }
    dependencies.map(_.toSbtWithVersion).zipWithIndex.map { case (str, i) => s":$i $str" }.foreach(println)
    SelectArtifact(dependencies, cache)
  }

  override def enterNumber(n: Int) = {
    println("type a query first")
    this
  }
}

case class SelectArtifact(dependencies: IndexedSeq[Dependency], override val cache: Seq[Dependency]) extends State {
  override def search(query: String) =
    SearchArtifact(cache).search(query)

  override def enterNumber(n: Int) =
    if (n < dependencies.size) {
      val dependency = dependencies(n)
      val decodedQuery = s"""q=g:${dependency.group}+AND+a:${dependency.artifact}"""
      val request = Http("http://search.maven.org/solrsearch/select?" + decodedQuery)
          .param("rows", "20")
          .param("wt", "json")
          .param("core", "gav")
      val response = request.asString
      var versions = (Json.parse(response.body) \ "response" \ "docs").as[JsArray].value.toIndexedSeq.map(_.as[JsObject]).map { obj =>
        obj \ "v" toString()
      }
      findLatestRelease(versions).foreach(versions +:= _)
      versions.zipWithIndex.map { case (str, i) => s":$i $str" }.foreach(println)
      SelectVersion(dependency, versions, cache)
    } else {
      println("incorrect number")
      this
    }

  private def findLatestRelease(versions: Seq[String]) = {
    val version = "^\"[\\d.]+\"$".r
    versions.filter(version.findFirstIn(_).isDefined).reduceOption((a, b) => if (a.compareTo(b) < 0) b else a)
  }
}

case class SelectVersion(dependency: Dependency, versions: IndexedSeq[String], override val cache: Seq[Dependency]) extends State {
  override def search(query: String) =
    SearchArtifact(cache).search(query)

  override def enterNumber(n: Int) =
    if (n < versions.size) {
      val version = versions(n)
      val newDependency = dependency.copy(version = version)
      SearchArtifact(cache :+ newDependency)
    } else {
      println("incorrect number")
      this
    }
}

case class Dependency(group: String, artifact: String, version: String) {
  def toSbt =
    if (scalaVersion.isDefined)
      s"""$group %% "${splitArtifact._1}" % $version"""
    else
      toSbtWithVersion

  def toSbtWithVersion = s"""$group %% $artifact % $version"""

  def scalaVersion = splitArtifact._2

  private val nameVersion = "^\"(.+)_([\\d.]+)\"$".r

  private def splitArtifact = artifact match {
    case nameVersion(name, v) =>
      (name, Some(v))
    case str => (str, None)
  }
}