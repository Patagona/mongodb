name := "mongodb"

organization in ThisBuild := "patagona"
version in ThisBuild := Versions.thisLibrary
scalaVersion in ThisBuild := Versions.scala
resolvers in ThisBuild ++= Seq[Resolver](
  "TypesafeRepository" at "http://repo.typesafe.com/typesafe/releases", // used for anorm
  s3resolver.value("Patagona Releases resolver", s3("patagona.repository/build_artifacts/master/")).withIvyPatterns
)
parallelExecution in ThisBuild := true
parallelExecution in (ThisBuild, Test) := true

// Jackson needed for log4j2 YAML config
libraryDependencies ++= Seq(
  "joda-time"                        % "joda-time"               % Versions.jodaTime,
  "org.scalatest"                    %% "scalatest"              % Versions.scalatest % "test",
  "org.scalacheck"                   %% "scalacheck"             % Versions.scalaCheck % "test",
  "org.scalatest"                    %% "scalatest"              % Versions.scalatest % "test",
  "org.mongodb.scala"                %% "mongo-scala-driver"     % Versions.mongoScalaDriver,
  "org.json4s"                       %% "json4s-core"            % Versions.json4s,
  "org.json4s"                       %% "json4s-ext"             % Versions.json4s,
  "org.scalacheck"                   %% "scalacheck"             % Versions.scalaCheck % "test",
  "org.slf4j"                        % "slf4j-api"               % Versions.slf4j,
  "org.apache.logging.log4j"         % "log4j-api"               % Versions.log4j,
  "org.apache.logging.log4j"         % "log4j-core"              % Versions.log4j,
  "org.apache.logging.log4j"         % "log4j-slf4j-impl"        % Versions.log4j,
  "com.fasterxml.jackson.core"       % "jackson-core"            % Versions.jackson,
  "com.fasterxml.jackson.core"       % "jackson-databind"        % Versions.jackson,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % Versions.jackson,
  "com.vlkan.log4j2"                 % "log4j2-logstash-layout"  % Versions.log4j2LogstashLayout,
  "net.codingwell"                   %% "scala-guice"            % "4.1.0" exclude ("com.google.code.findbugs", "jsr305"),
  "com.google.inject"                % "guice"                   % Versions.guice force (),
  "patagona"                         %% "core"                   % Versions.patagonaApi
)

Keys.fork in (ThisBuild, Test) := true

updateOptions := updateOptions.value.withCachedResolution(true)

testForkedParallel in (ThisBuild, Test) := true

javacOptions in ThisBuild ++= Seq("-encoding", "UTF-8")

javaOptions in (ThisBuild, Test) ++= Seq("-Xmx2G", "-Dfile.encoding=UTF-8")

addCommandAlias("cc", ";clean;compile")
addCommandAlias("fc", ";test:scalafmt;scalafmt")

publishMavenStyle in ThisBuild := false
publishArtifact in (ThisBuild, Test) := true
publishArtifact in Test := true
publishTo in ThisBuild := {
  val prefix =
    sys.props.get("branch.name").map("build_artifacts/" + _ + "/").getOrElse("unspecified_artifacts_location/")
  Some(s3resolver.value("Patagona " + prefix + " S3 bucket", s3("patagona.repository/" + prefix)).withIvyPatterns)
}

cleanKeepFiles ++= Seq("resolution-cache", "streams").map(target.value / _)
