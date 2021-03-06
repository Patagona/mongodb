package com.patagona.util.mongodb

import com.patagona.util.mongodb.dao.MongoDBDAO
import org.mongodb.scala.{Document, FindObservable, SingleObservable}
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.BulkWriteOptions
import org.mongodb.scala.model.UpdateOneModel
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.ExecutionContext.Implicits.global

class MongoDBDAOSpec extends FlatSpec with MustMatchers with MongoDBTest with MongoDBTestUtils {
  "MongoDBDAO" should "set fields in a safe way for mongodb" in withMongoDB { mdb =>
    val dao = new MongoDBDAO {
      val db = mdb
    }

    val nullSet = dao.safeSet("some" -> "some", "null" -> null) // scalastyle:ignore null
    val noneSet = dao.safeSet("some" -> "some", "none" -> None)
    val complexSet = dao.safeSet(
      "double" -> 2.33,
      "setOption" -> Some("value"),
      "string" -> "value",
      "none" -> None,
      "null" -> null // scalastyle:ignore null
    )
    val queries = (1L to 3L).map(gtin => BsonDocument("contractId" -> 10L, "gtin" -> gtin))

    val collection = mdb.getCollection("products")
    val bulkOperations = List(
      UpdateOneModel(queries(0), nullSet, UpdateOptions().upsert(true)),
      UpdateOneModel(queries(1), noneSet, UpdateOptions().upsert(true)),
      UpdateOneModel(queries(2), complexSet, UpdateOptions().upsert(true))
    )
    collection.bulkWrite(bulkOperations, BulkWriteOptions().ordered(false))

    whenFindCompleted(collection.find(queries(0))) { docs =>
      docs.foreach { doc =>
        doc.contains("none") must be(false)
        doc.getString("none") must be(None)
        doc.getString("some") must be('defined)
      }
    }

    whenFindCompleted(collection.find(queries(1))) { docs =>
      docs.foreach { doc =>
        doc.contains("null") must be(false)
        doc.getString("null") must be(None)
        doc.getString("some") must be('defined)
      }
    }

    whenFindCompleted(collection.find(queries(2))) { docs =>
      docs.foreach { doc =>
        doc.getDouble("double") must be('defined)
        doc.getString("setOption") must be('defined)
        doc.getString("string") must be('defined)
        doc.contains("null") must be(false)
        doc.contains("none") must be(false)
        doc.getString("none") must be(None)
        doc.getString("null") must be(None)
      }
    }
  }

  it should "unset fields" in withMongoDB { mdb =>
    val dao = new MongoDBDAO {
      val db = mdb
    }

    val keyField = "key"
    val field = "field"

    val query = BsonDocument(keyField -> 1)
    val collection = mdb.getCollection("products")

    val setOperation = dao.safeSet(field -> 1)
    val updateF = collection.updateOne(query, setOperation, UpdateOptions().upsert(true))
    whenSingleCompleted(updateF) { _ =>
      }

    whenFindCompleted(collection.find(query)) { docs =>
      docs must have size 1
      docs.head.getInteger(field) must be(1)
    }

    whenSingleCompleted(collection.updateOne(query, dao.safeSet(field -> None))) { _ =>
      }
    whenFindCompleted(collection.find(query)) { docs: Seq[org.mongodb.scala.Document] =>
      docs must have size 1
      docs.head.keys must not contain field
    }
  }
}
