package dcomposer

sealed trait State {

  def search(query: String): State

  def enterNumber(n: Int): State

  def generateSbt: String = {
    val versions = cache.flatMap(_.scalaVersion).toSet
    val isOneScalaVersion = versions.size == 1
    val prefix = if (isOneScalaVersion)
      s"""scalaVersion := "${ds.fullScalaVersion(versions.head)}"\n\n"""
    else
      ""
    val dependencies = {
      if (isOneScalaVersion)
        cache.map(_.toSbt)
      else
        cache.map(_.toSbtWithVersion)
    }.mkString(",\n")
    s"${prefix}libraryDependencies ++= Seq(\n$dependencies\n)"
  }

  def generateCbt: String = {
    val versions = cache.flatMap(_.scalaVersion).toSet
    val isOneScalaVersion = versions.size == 1
    val prefix = if (isOneScalaVersion)
      s"""override def defaultScalaVersion = "${ds.fullScalaVersion(versions.head)}"\n\n"""
    else
      ""
    val dependencies = {
      if (isOneScalaVersion)
        cache.map(_.toCbt)
      else
        cache.map(_.toCbtWithVersion)
    }.mkString(",\n")
    s"""${prefix}override def dependencies = {
       |  super.dependencies ++ Resolver(mavenCentral).bind(
       |$dependencies
       |  )
       |}""".stripMargin
  }

  def generateMvn: String = {
    val dependencies = cache.map(_.toMvn).mkString("\n")
    s"""<dependencies>
       |$dependencies
       |</dependencies>""".stripMargin
  }

  def generateGradle: String = {
    val dependencies = cache.map(_.toGradle).mkString("\n")
    s"""dependencies {
       |$dependencies
       |}""".stripMargin
  }

  protected val ds: DataSource

  protected val cache: Seq[Dependency]
}

case class SearchArtifact(
    override val ds: DataSource,
    override val cache: Seq[Dependency] = Seq()) extends State {

  override def search(query: String) = {
    val dependencies = ds.searchDependency(query)
    val artifactWidth = if (dependencies.isEmpty) 0 else dependencies.map(_.artifact.length).max
    dependencies.map(_.readable(artifactWidth)).zipWithIndex.map { case (str, i) => s"  :$i\t$str" }.foreach(println)
    SelectArtifact(ds, dependencies, cache)
  }

  override def enterNumber(n: Int) = {
    println("  type a query first")
    this
  }
}

case class SelectArtifact(
    override val ds: DataSource,
    dependencies: IndexedSeq[Dependency],
    override val cache: Seq[Dependency]) extends State {

  override def search(query: String) =
    SearchArtifact(ds, cache).search(query)

  override def enterNumber(n: Int) =
    if (n < dependencies.size) {
      val dependency = dependencies(n)
      val versions = ds.searchVersion(dependency)
      versions.zipWithIndex.map { case (str, i) => s"  :$i\t$str" }.foreach(println)
      SelectVersion(ds, dependency, versions, cache)
    } else {
      println("  incorrect number")
      this
    }
}

case class SelectVersion(
    override val ds: DataSource,
    dependency: Dependency,
    versions: IndexedSeq[String],
    override val cache: Seq[Dependency]) extends State {

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

case class Dependency(group: String, artifact: String, version: String) {

  def readable(artifactWidth: Int) = {
    val filler = new String(Array.fill(artifactWidth - artifact.length)(' '))
    s"$artifact$filler - $group - $version"
  }

  def toSbt =
    if (scalaVersion.isDefined)
      s"""  "$group" %% "${splitArtifact._1}" % "$version""""
    else
      toSbtWithVersion

  def toSbtWithVersion = s"""  "$group" % "$artifact" % "$version""""

  def toCbt =
    if (scalaVersion.isDefined)
      s"""    ScalaDependency("$group", "${splitArtifact._1}", "$version")"""
    else
      toCbtWithVersion

  def toCbtWithVersion = s"""    MavenDependency("$group", "$artifact", "$version")"""

  def scalaVersion = splitArtifact._2

  def toMvn =
    s"""  <dependency>
       |    <groupId>$group</groupId>
       |    <artifactId>$artifact</artifactId>
       |    <version>$version</version>
       |  </dependency>""".stripMargin

  def toGradle =
    s"  compile(group: '$group', name: '$artifact', version: '$version')"

  private val nameVersion = "^(.+)_([^_]+)$".r

  private def splitArtifact = artifact match {
    case nameVersion(name, v) =>
      (name, Some(v))
    case str => (str, None)
  }
}