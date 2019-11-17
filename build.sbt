name := "excel-parser"

version := "0.1"

scalaVersion := "2.13.1"
val akkaVersion     = "2.5.25"
val akkaHttpVersion = "10.1.9"
val circeVersion    = "0.12.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"          % akkaVersion,
  "com.typesafe.akka"  %% "akka-testkit"        % akkaVersion % Test,
  "com.typesafe.akka"  %% "akka-stream"         % akkaVersion,
  "com.typesafe.akka"  %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka"  %% "akka-http"           % akkaHttpVersion,
  "com.typesafe.akka"  %% "akka-http-testkit"   % akkaHttpVersion,
  "io.circe"           %% "circe-core"          % circeVersion,
  "io.circe"           %% "circe-generic"       % circeVersion,
  "io.circe"           %% "circe-parser"        % circeVersion,
  "de.heikoseeberger"  %% "akka-http-circe"     % "1.29.1",
  "ch.megard"          %% "akka-http-cors"      % "0.4.1",
  "org.apache.poi"     % "poi"                  % "4.1.0",
  "org.apache.poi"     % "poi-ooxml"            % "4.1.0",
  "org.postgresql"     % "postgresql"           % "42.2.6",
  "com.typesafe.slick" %% "slick"               % "3.3.2",
  "com.typesafe.slick" %% "slick-hikaricp"      % "3.3.2",
  "ch.qos.logback"     % "logback-classic"      % "1.1.2",
  "joda-time"          % "joda-time"            % "2.10.2",
  "org.joda"           % "joda-convert"         % "2.2.1",
  "com.pauldijou"      %% "jwt-core"            % "4.2.0",
  "org.mindrot"        % "jbcrypt"              % "0.4"
)
// resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/"
