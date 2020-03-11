// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe/Sonatype repositories
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += Resolver.url("bintray-sbt-plugins", url("https://dl.bintray.com/sbt/sbt-plugin-releases/"))(
  Resolver.ivyStylePatterns
)

// s3-sbt-plugin
resolvers += "Era7 maven releases" at "https://s3-eu-west-1.amazonaws.com/releases.era7.com"

addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.15.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.10.2")

addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.8.0")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "1.15")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

lazy val root = (project in file(".")).dependsOn(codeStyleSettingsPlugin)
lazy val codeStyleSettingsPlugin = uri(
  "https://github.com/Patagona/code-style-settings-plugin.git#3d5f25cf176e214560a07b107f9cf7538f7cbb22"
)
