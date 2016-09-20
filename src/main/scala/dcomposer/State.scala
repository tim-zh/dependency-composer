package dcomposer

sealed trait State {
  def search(query: String): State

  def enterNumber(n: Int): State

  def generateSbt: String = {
    val versions = cache.flatMap(_.scalaVersion).toSet
    val isOneScalaVersion = versions.size == 1
    val prefix = if (isOneScalaVersion)
      s"scalaVersion := ${versions.head}\n\n"
    else
      ""
    val dependencies = cache.map { d =>
      "\n  " + (if (isOneScalaVersion) d.toSbt else d.toSbtWithVersion)
    }.mkString(",")
    s"${prefix}libraryDependencies ++= Seq($dependencies)"
  }

  val cache: Seq[Dependency]
}

case class SearchArtifact(ds: DataSource, override val cache: Seq[Dependency] = Seq()) extends State {
  override def search(query: String) = {
    val dependencies = ds.searchDependency(query)
    dependencies.map(_.toSbtWithVersion).zipWithIndex.map { case (str, i) => s"  :$i $str" }.foreach(println)
    SelectArtifact(ds, dependencies, cache)
  }

  override def enterNumber(n: Int) = {
    println("  type a query first")
    this
  }
}

case class SelectArtifact(ds: DataSource, dependencies: IndexedSeq[Dependency], override val cache: Seq[Dependency]) extends State {
  override def search(query: String) =
    SearchArtifact(ds, cache).search(query)

  override def enterNumber(n: Int) =
    if (n < dependencies.size) {
      val dependency = dependencies(n)
      val versions = ds.searchVersion(dependency)
      versions.zipWithIndex.map { case (str, i) => s"  :$i $str" }.foreach(println)
      SelectVersion(ds, dependency, versions, cache)
    } else {
      println("  incorrect number")
      this
    }
}

case class SelectVersion(ds: DataSource, dependency: Dependency, versions: IndexedSeq[String], override val cache: Seq[Dependency]) extends State {
  override def search(query: String) =
    SearchArtifact(ds, cache).search(query)

  override def enterNumber(n: Int) =
    if (n < versions.size) {
      val version = versions(n)
      val newDependency = dependency.copy(version = version)
      println("  done")
      SearchArtifact(ds, cache :+ newDependency)
    } else {
      println("  incorrect number")
      this
    }
}

case class Dependency(group: String, artifact: String, version: String, allVersions: IndexedSeq[String] = IndexedSeq()) {
  def toSbt =
    if (scalaVersion.isDefined)
      s""""$group" %% "${splitArtifact._1}" % "$version""""
    else
      toSbtWithVersion

  def toSbtWithVersion = s""""$group" % "$artifact" % "$version""""

  def scalaVersion = splitArtifact._2

  private val nameVersion = "^(.+)_([^_]+)$".r

  private def splitArtifact = artifact match {
    case nameVersion(name, v) =>
      (name, Some(v))
    case str => (str, None)
  }
}