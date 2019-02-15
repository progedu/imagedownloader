name := "imagedownloader"

version := "1.0"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.17",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.17" % Test,
  "com.squareup.okhttp3" % "okhttp" % "3.11.0",
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.akka" %% "akka-stream" % "2.5.20"
)

mainClass in assembly := Some("jp.ed.nnn.imagedownloader.Main")