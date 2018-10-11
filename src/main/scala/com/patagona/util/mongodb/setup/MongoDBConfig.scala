package com.patagona.util.mongodb.setup

import java.util.concurrent.TimeUnit

import com.mongodb.Block
import com.mongodb.ReadPreference
import com.mongodb.ServerAddress
import com.mongodb.connection.ClusterSettings
import com.mongodb.connection.SocketSettings
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoClientSettings

import collection.JavaConverters._

case class MongoDBConfig(host: String, port: Int, db: String, replicaSet: Option[String] = None)

object MongoDBConfig {
  def fromSystemProperties: MongoDBConfig =
    MongoDBConfig(
      sys.props.getOrElse(
        "mongodb.host",
        sys.env.getOrElse("mongodb.host", throw new RuntimeException("Missing parameter 'mongodb.host'"))
      ),
      sys.props.getOrElse("mongodb.port", sys.env.getOrElse("mongodb.port", "27017")).toInt,
      sys.props.getOrElse(
        "mongodb.db",
        sys.env.getOrElse("mongodb.db", throw new RuntimeException("Missing parameter 'mongodb.db'"))
      ),
      Option(sys.props.getOrElse[String]("mongodb.replicaSet", null)) // scalastyle:ignore
    )

  def createAsyncClient(config: MongoDBConfig): MongoClient = {
    val hosts = config.host.split(",")

    if (hosts.length > 1) {
      require(config.replicaSet.nonEmpty, "A replicaset must be specified when using multiple servers")
    }

    val addresses = hosts.map(host => new ServerAddress(host.trim, config.port))

    val settings = MongoClientSettings
      .builder()
      .applyToClusterSettings(new Block[ClusterSettings.Builder]() {
        override def apply(b: ClusterSettings.Builder): Unit = b.hosts(addresses.toList.asJava)
      })
      .applyToSocketSettings(new Block[SocketSettings.Builder]() {
        override def apply(b: SocketSettings.Builder): Unit =
          b.connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS)
      })

    val clusteredSettings = config.replicaSet match {
      case Some(replicaSet) =>
        settings
          .applyToClusterSettings(new Block[ClusterSettings.Builder]() {
            override def apply(b: ClusterSettings.Builder): Unit = b.requiredReplicaSetName(replicaSet)
          })
          .readPreference(ReadPreference.secondaryPreferred())
          .build()
      case None =>
        settings.build()
    }

    MongoClient(clusteredSettings)
  }
}
