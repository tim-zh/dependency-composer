name := "dependency-composer"

version := "1.0"

scalaVersion := "2.11.8"

assemblyOutputPath in assembly := file("./dc.jar")

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "com.typesafe.play" %% "play-json" % "2.3.8"
)