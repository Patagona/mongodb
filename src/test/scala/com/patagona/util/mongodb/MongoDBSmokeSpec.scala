package com.patagona.util.mongodb

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.collection.immutable.Document
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers

import scala.concurrent.ExecutionContext.Implicits.global

class MongoDBSmokeSpec extends FlatSpec with MustMatchers with MongoDBTest with MongoDBTestUtils {
  "MongoDB" should "be available" in withMongoDB { db =>
    val collection = db.getCollection("test-collection")
    whenSingleCompleted(collection.insertOne(BsonDocument("_id" -> "12345", "test" -> "works"))) { _ =>
      whenFindCompleted(collection.find()) { docs =>
        docs.size must be(1)

        docs.head must be(Document("_id" -> "12345", "test" -> "works"))
      }
    }
  }
}
