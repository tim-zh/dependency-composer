#dependency-composer

console rest client for browsing Maven Central (and possibly Bintray) and generating dependency list (for sbt)

###Setup:

```
$ sbt assembly
```

###Example usage:

```
$ java -jar dc.jar
Usage:
  - type an artifact name pattern
  - type :number (e.g. :1) to choose from search result
  - type :number again to choose version from search result, :0 is the latest release
  - type :go to generate sbt libraryDependencies from chosen results
  - type :x to exit
joda-time
  :0 "joda-time" % "joda-time" % "2.9.4"
  :1 "joda-time" % "joda-time-hibernate" % "1.4"
  ...
:0
  :0 2.9.4
  :1 2.9.3
  ...
:0
  done
play-json
  :0 "com.typesafe.play" % "play-json_2.11" % "2.5.8"
  :1 "de.heikoseeberger" % "akka-http-play-json_2.11" % "1.10.0"
  ...
:0
  :0 2.5.8
  :1 2.5.7
  ...
:1
  done
:sbt
scalaVersion := 2.11

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.9.4",
  "com.typesafe.play" %% "play-json" % "2.5.7")
:x
$
```