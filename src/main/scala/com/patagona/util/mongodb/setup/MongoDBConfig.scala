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

case class MongoDBConfig(host: String, port: Int, db: String, replication: Boolean = true)

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
      )
    )

  def createAsyncClient(config: MongoDBConfig): MongoClient = {
    val address = new ServerAddress(config.host, config.port)

    val settings = MongoClientSettings
      .builder()
      .applyToClusterSettings(new Block[ClusterSettings.Builder]() {
        override def apply(b: ClusterSettings.Builder): Unit = b.hosts(List(address).asJava)
      })
      .applyToSocketSettings(new Block[SocketSettings.Builder]() {
        override def apply(b: SocketSettings.Builder): Unit =
          b.connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS)
      })

    val clusteredSettings = if (config.replication) {
      settings
        .applyToClusterSettings(new Block[ClusterSettings.Builder]() {
          override def apply(b: ClusterSettings.Builder): Unit = b.requiredReplicaSetName("rs0")
        })
        .readPreference(ReadPreference.secondaryPreferred())
        .build()
    } else {
      settings.build()
    }

    MongoClient(clusteredSettings)
  }
}
