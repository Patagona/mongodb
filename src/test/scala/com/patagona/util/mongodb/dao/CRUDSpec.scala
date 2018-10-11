package com.patagona.util.mongodb.dao

import com.patagona.util.mongodb.MongoDBAsyncTest
import org.joda.time.DateTime
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.collection.immutable.Document
import org.scalatest.AsyncWordSpec
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CRUDSpec extends AsyncWordSpec with MustMatchers with MongoDBAsyncTest with ScalaFutures {
  val ec: ExecutionContext = global
  val crud: CRUD = new CRUD {}

  "CRUD" when {
    "reading one object" should {
      "return none if the collection is empty" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "emptyCollection", "1")
        crud.read[String](Map("someKey" -> "someValue"))(_ => "").map { result =>
          result must be(None)
        }
      }

      "return the selected object" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "emptyCollection", "1")

        val expectedValue = "TestString"

        val doc = Document("_someKey" -> "someValue", "data" -> expectedValue, "schemaVersion" -> "1")

        context.collection
          .insertOne(doc)
          .toFuture
          .flatMap { _ =>
            crud.read[String](Map("someKey" -> "someValue"))(_ => "")
          }
          .map { retrievedValue =>
            retrievedValue must be(Some(Map("someKey" -> "someValue") -> expectedValue))
          }
      }
    }
    "counting many objects" should {
      "return all expected values" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")
        val expectedValues = (1 until 10).map(_.toString)
        val unexpectedValues = (10 until 20).map(_.toString)
        val allValues = expectedValues ++ unexpectedValues

        val docs = allValues.map { value =>
          Document("_someKey" -> value, "data" -> value, "schemaVersion" -> "1")
        }

        context.collection
          .insertMany(docs)
          .toFuture
          .flatMap { _ =>
            crud.countMany(Map("someKey" -> expectedValues))
          }
          .map { expectedValueCount =>
            expectedValueCount must be(9)
          }
      }
    }

    "reading many objects" should {
      "return all expected values" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")
        val expectedValues = (1 until 10).map(_.toString)
        val unexpectedValues = (10 until 20).map(_.toString)
        val allValues = expectedValues ++ unexpectedValues

        val docs = allValues.map { value =>
          Document("_someKey" -> value, "data" -> value, "schemaVersion" -> "1")
        }

        context.collection
          .insertMany(docs)
          .toFuture
          .flatMap { _ =>
            crud.readMany[String](Map("someKey" -> expectedValues), 0, allValues.size)(_.deserialize[String])
          }
          .map { retrievedValues =>
            val expectedRetrievedValues = expectedValues.map { value =>
              Map("someKey" -> value) -> value
            }

            retrievedValues must be(expectedRetrievedValues)
          }
      }
      "extract additional keys" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")
        val values = (1 until 10).map(_.toString)

        val docs = values.map { value =>
          Document("_someKey" -> value, "_additionalKey" -> value, "data" -> value, "schemaVersion" -> "1")
        }

        context.collection
          .insertMany(docs)
          .toFuture
          .flatMap { _ =>
            crud.readMany[String](Map("someKey" -> values), 0, values.size)(_.deserialize[String])
          }
          .map { retrievedValues =>
            val expectedRetrievedValues = values.map { value =>
              Map("someKey" -> value, "additionalKey" -> value) -> value
            }

            retrievedValues must be(expectedRetrievedValues)
          }
      }
    }

    "inserting one object" should {
      "insert the document" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")
        crud
          .insert[String](Map("key" -> "value"))("new data")(_.serialize)
          .flatMap { _ =>
            crud.read[String](Map("key" -> "value"))(_ => "")
          }
          .map { retrievedValue =>
            retrievedValue must be(Some(Map("key" -> "value"), "new data"))
          }
      }

      "set the creation date" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")
        crud
          .insert[String](Map("key" -> "value"))("new data")(_.serialize)
          .flatMap { _ =>
            context.collection
              .find(Document("_key" -> "value"))
              .first()
              .toFuture
          }
          .map { doc =>
            val creationDate = Option(doc.getString("creationDate")).map(DateTime.parse)
            val updateDate = Option(doc.getString("updateDate")).map(DateTime.parse)

            creationDate must be('defined)
            creationDate must be(updateDate)
          }
      }
    }

    "updating the keys of an existing object" should {
      "add new keys" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")

        crud
          .insert[String](Map("key" -> "value"))("new data")(_.serialize)
          .flatMap { _ =>
            crud.updateKeys(Map("key" -> "value"), Map("other_key" -> "other_value"))
          }
          .flatMap { _ =>
            crud.read[String](Map("key" -> "value", "other_key" -> "other_value"))(_ => "")
          }
          .map { retrievedValue =>
            retrievedValue must be(Some(Map("key" -> "value", "other_key" -> "other_value"), "new data"))
          }
      }

      "update existing keys" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")

        crud
          .insert[String](Map("key" -> "value"))("new data")(_.serialize)
          .flatMap { _ =>
            crud.updateKeys(Map("key" -> "value"), Map("key" -> "other_value"))
          }
          .flatMap { _ =>
            crud.read[String](Map("key" -> "other_value"))(_ => "")
          }
          .map { retrievedValue =>
            retrievedValue must be(Some(Map("key" -> "other_value"), "new data"))
          }
      }
    }

    "upserting one object" should {
      "insert the document" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")
        crud
          .upsert[String](Map("key" -> "value"))("new data")(_.serialize)
          .flatMap { _ =>
            crud.read[String](Map("key" -> "value"))(_ => "")
          }
          .map { retrievedValue =>
            retrievedValue must be(Some(Map("key" -> "value"), "new data"))
          }
      }

      "set creationDate only once" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")

        crud
          .upsert[String](Map("key" -> "value"))("new data")(_.serialize)
          .flatMap { _ =>
            context.collection
              .find(Document("_key" -> "value"))
              .first()
              .toFuture
              .map(
                doc =>
                  Option(doc.getString("creationDate"))
                    .getOrElse(throw new RuntimeException("creationDate field was not found"))
              )
              .map(DateTime.parse)
          }
          .flatMap { creationDate =>
            crud
              .upsert[String](Map("key" -> "value"))("new data")(_.serialize)
              .flatMap(_ => context.collection.find(Document("_key" -> "value")).first().toFuture)
              .map { document =>
                Option(document.getString("creationDate")).map(DateTime.parse) must be(Some(creationDate))
              }
          }
      }

      "update the updateDate" in withMongoDB { db =>
        implicit val context: DBContext = DBContext(db, "test", "1")

        crud
          .upsert[String](Map("key" -> "value"))("new data")(_.serialize)
          .flatMap { _ =>
            context.collection
              .find(Document("_key" -> "value"))
              .first()
              .toFuture
              .map(
                doc =>
                  Option(doc.getString("updateDate"))
                    .getOrElse(throw new RuntimeException("updateDate field was not found"))
              )
              .map(DateTime.parse)
          }
          .flatMap { updateDate =>
            crud
              .upsert[String](Map("key" -> "value"))("new data")(_.serialize)
              .flatMap(_ => context.collection.find(Document("_key" -> "value")).first().toFuture)
              .map { document =>
                val comparison =
                  Option(document.getString("updateDate")).map(DateTime.parse).map(_.compareTo(updateDate))
                comparison.get must be > 0
              }
          }
      }

      "not accept empty keys" in {
        recoverToSucceededIf[IllegalArgumentException] {
          implicit val context: DBContext = DBContext(null, "", "1") // scalastyle:ignore null
          Future().flatMap(_ => crud.upsert[String](Map())("new data")(_.serialize))
        }
      }

      "not accept 'id' as key" in {
        recoverToSucceededIf[IllegalArgumentException] {
          implicit val context: DBContext = DBContext(null, "", "1") // scalastyle:ignore null
          Future().flatMap(_ => crud.upsert[String](Map("id" -> "1234"))("new data")(_.serialize))
        }
      }
    }

    "building the search query" should {
      "escape keys with a '_' as prefix" in {
        val keys = Map("key1" -> "v1", "_key2" -> "v2")
        crud.buildQuery(keys) must be(Document("_key1" -> "v1", "__key2" -> "v2"))
      }
      "not accept empty keys" in {
        recoverToSucceededIf[IllegalArgumentException] {
          Future { crud.buildQuery(Map("" -> "v1")) }
        }
      }
      "not accept empty values" in {
        recoverToSucceededIf[IllegalArgumentException] {
          Future { crud.buildQuery(Map("key" -> "")) }
        }
      }
    }

    implicit def extendMongoDBObject(o: BsonValue): ExtendedBsonValue = ExtendedBsonValue(o)

    implicit def extendedObject(o: Any): ExtendedObject = ExtendedObject(o)

    "extracting data from the document" should {

      "extract arrays" in {
        val expected = Seq("one", "two")
        crud.extractData[Seq[String]](_.deserialize[Seq[String]])(Document("data" -> expected.serialize)) must be(
          expected
        )
      }

      "extract complex objects" in {
        val expected = ComplexObject("someValue")
        crud.extractData[ComplexObject](_.deserialize[ComplexObject])(Document("data" -> expected.serialize)) must be(
          expected
        )
      }
    }
  }
}

case class ComplexObject(value: String)
