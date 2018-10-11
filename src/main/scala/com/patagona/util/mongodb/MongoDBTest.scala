package com.patagona.util.mongodb

import java.util.UUID

import com.patagona.util.mongodb.setup.MongoDBConfig
import org.mongodb.scala.MongoDatabase

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

trait MongoDBTest {
  def withMongoDB[A](f: MongoDatabase => A): A = withMongoDBSetup { (client, _) =>
    f(client)
  }

  def withMongoDBConfig[A](f: MongoDBConfig => A): A = withMongoDBSetup { (_, config) =>
    f(config)
  }

  def withMongoDBSetup[A](f: (MongoDatabase, MongoDBConfig) => A): A = {
    val dbName = "test-" + UUID.randomUUID().toString
    val config =
      MongoDBConfig(sys.env.getOrElse("patagona_test_mongodb_host", "localhost"), 27017, dbName)
    val mongoClient = MongoDBConfig.createAsyncClient(config)

    try {
      f(mongoClient.getDatabase(dbName), config)
    } finally {
      Await.ready(mongoClient.getDatabase(dbName).drop().toFuture, 5 seconds)
      mongoClient.close()
    }
  }

  def mongoDBModule(db: MongoDatabase): MongoDBTestModule = new MongoDBTestModule(db)

  def withDAO[A](testBlock: A => Any)(implicit daoBuilder: MongoDatabase => A): Any = {
    withMongoDB(db => testBlock(daoBuilder(db)))
  }
}
