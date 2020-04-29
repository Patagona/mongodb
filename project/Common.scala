import sbt._

object Versions {
  def thisLibrary: String = sys.props.getOrElse("application.version", "9999.0.0")
  def mongoScalaDriver: String = "2.4.1"
  def scalatest: String = "3.0.1"
  def scala: String = "2.11.8"
  def json4s: String = "3.2.11"
  def scalaCheck: String = "1.13.5"
  def jodaTime: String = "2.3"
  def slf4j: String = "1.7.25"
  def log4j: String = "2.11.0"
  def jackson: String = "2.8.11"
  def log4j2LogstashLayout: String = "0.10"
  def guice: String = "4.1.0"
  val patagonaApi = "0.0.3725"
}
