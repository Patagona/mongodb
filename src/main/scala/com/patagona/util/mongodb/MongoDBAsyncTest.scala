package com.patagona.util.mongodb

import java.util.UUID

import com.patagona.util.mongodb.setup.MongoDBConfig
import org.mongodb.scala.{MongoCollection, MongoDatabase}
import org.mongodb.scala.bson.BsonValue

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

trait MongoDBAsyncTest {
  implicit def ec: ExecutionContext

  def withMongoDBCollection[A](collection: String)(f: MongoCollection[BsonValue] => Future[A]): Future[A] = {
    withMongoDBSetup { (client, _) =>
      f(client.getCollection[BsonValue](collection))
    }
  }

  def withMongoDB[A](f: MongoDatabase => Future[A]): Future[A] = withMongoDBSetup { (client, _) =>
    f(client)
  }

  def withMongoDBConfig[A](f: MongoDBConfig => Future[A]): Future[A] = withMongoDBSetup { (_, config) =>
    f(config)
  }

  def withMongoDBSetup[A](f: (MongoDatabase, MongoDBConfig) => Future[A]): Future[A] = {
    val dbName = "test-" + UUID.randomUUID().toString
    val config =
      MongoDBConfig(sys.env.getOrElse("patagona_test_mongodb_host", "localhost"), 27017, dbName)
    val mongoClient = MongoDBConfig.createAsyncClient(config)

    val future = Future {}.flatMap { _ =>
      f(mongoClient.getDatabase(dbName), config)
    }

    future.onComplete { _ =>
      Await.ready(mongoClient.getDatabase(dbName).drop().toFuture, 5 seconds)
      mongoClient.close()
    }

    future
  }

  def mongoDBModule(db: MongoDatabase): MongoDBTestModule = new MongoDBTestModule(db)

  def withDAO[A](testBlock: A => Future[Any])(implicit daoBuilder: MongoDatabase => A): Future[Any] = {
    withMongoDB(db => testBlock(daoBuilder(db)))
  }
}
