name := "stock-management-service"

version := "0.1"

scalaVersion := "2.13.1"

test in assembly := {}
assemblyJarName in assembly := s"app-assembly.jar"
assemblyMergeStrategy in assembly := {
  case PathList("reference.conf")          => MergeStrategy.concat
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _                                   => MergeStrategy.first
}

val akkaVersion     = "2.6.1"
val akkaHttpVersion = "10.1.11"
val circeVersion    = "0.12.3"
val slickVersion    = "3.3.2"

testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential")
testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "timeFactor", "10")

libraryDependencies ++= Seq(
  "com.typesafe.akka"      %% "akka-actor"          % akkaVersion,
  "com.typesafe.akka"      %% "akka-testkit"        % akkaVersion % Test,
  "com.typesafe.akka"      %% "akka-stream"         % akkaVersion,
  "com.typesafe.akka"      %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka"      %% "akka-http"           % akkaHttpVersion,
  "com.typesafe.akka"      %% "akka-http-testkit"   % akkaHttpVersion % Test,
  "io.circe"               %% "circe-core"          % circeVersion,
  "io.circe"               %% "circe-generic"       % circeVersion,
  "io.circe"               %% "circe-parser"        % circeVersion,
  "de.heikoseeberger"      %% "akka-http-circe"     % "1.29.1",
  "ch.megard"              %% "akka-http-cors"      % "0.4.1",
  "org.apache.poi"         % "poi"                  % "4.1.0",
  "org.apache.poi"         % "poi-ooxml"            % "4.1.0",
  "joda-time"              % "joda-time"            % "2.10.2",
  "org.joda"               % "joda-convert"         % "2.2.1",
  "com.pauldijou"          %% "jwt-core"            % "4.2.0",
  "org.mindrot"            % "jbcrypt"              % "0.4",
  "com.github.pureconfig"  %% "pureconfig"          % "0.12.1",
  "software.amazon.awssdk" % "aws-sdk-java"         % "2.10.35",
  "ch.megard"              %% "akka-http-cors"      % "0.4.1"
)

// Test
libraryDependencies ++= Seq(
  "org.specs2"    %% "specs2-core" % "4.6.0" % Test,
  "org.scalamock" %% "scalamock"   % "4.4.0" % Test
)

// cats
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.0.0"
)

// Email
libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-email" % "1.5"
)

// db
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick"          % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "org.postgresql"     % "postgresql"      % "42.2.6",
  "org.flywaydb"       % "flyway-core"     % "6.2.0"
)

// logging
libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2",
  "ch.qos.logback"             % "logback-classic" % "1.1.2"
)
