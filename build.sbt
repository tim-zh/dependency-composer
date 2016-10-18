name := "dependency-composer"

version := "1.0"

scalaVersion := "2.11.8"

assemblyOutputPath in assembly := file("./dc.jar")

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.3",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.3",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.8.3",
  "org.eclipse.jetty" % "jetty-server" % "9.3.12.v20160915"
)