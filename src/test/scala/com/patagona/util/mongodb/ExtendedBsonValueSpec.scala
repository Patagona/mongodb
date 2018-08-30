package com.patagona.util.mongodb

import com.patagona.util.mongodb.dao.ExtendedBsonValue
import com.patagona.util.mongodb.dao.ExtendedObject
import com.patagona.util.mongodb.dao.MongoFormats
import org.joda.time.DateTime
import org.json4s.Extraction
import org.mongodb.scala.bson.BsonArray
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.BsonValue
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures

class ExtendedBsonValueSpec
    extends FlatSpec with MustMatchers with MongoDBTest with MongoDBTestUtils with ScalaFutures {
  val dbObject = ExtendedObject(null) // scalastyle:ignore null
  implicit val formats = MongoFormats.baseFormatsWithMongoDateTime

  "ExtendedObject" must "escape dots with @@ in keys in maps" in {
    val json = dbObject.escape(Extraction.decompose(Map("a.b" -> Map("a.b" -> "a.b"))))
    json must be(Extraction.decompose(Map("a@@b" -> Map("a@@b" -> "a.b"))))
  }

  "ExtendedObject" must "not escape dots in strings" in {
    val json = dbObject.escape(Extraction.decompose("a.b"))
    json must be(Extraction.decompose("a.b"))
  }

  "ExtendedObject" must "not escape dots in integers" in {
    val json = dbObject.escape(Extraction.decompose(1))
    json must be(Extraction.decompose(1))
  }

  "ExtendedObject" must "not escape dots in floats" in {
    val json = dbObject.escape(Extraction.decompose(1.2))
    json must be(Extraction.decompose(1.2))
  }

  val mongoDBObject = ExtendedBsonValue(null) // scalastyle:ignore null

  "ExtendedMongoDBObject" must "unescape dots in maps" in {
    val json = mongoDBObject.unescape(Extraction.decompose(Map("a@@b" -> "a.b")))
    json must be(Extraction.decompose(Map("a.b" -> "a.b")))
  }

  it must "handle simple strings when unescaping" in {
    val json = mongoDBObject.unescape(Extraction.decompose("a@@b"))
    json must be(Extraction.decompose("a@@b"))
  }

  it must "handle simple integers when unescaping" in {
    val json = mongoDBObject.unescape(Extraction.decompose(1))
    json must be(Extraction.decompose(1))
  }

  it must "handle simple floats when unescaping" in {
    val json = mongoDBObject.unescape(Extraction.decompose(1.2))
    json must be(Extraction.decompose(1.2))
  }

  it must "escape/unescape dots in keys" in {
    val serializableObject = ExtendedObject(Map("a.b" -> 1.2))
    val serializedObject = serializableObject.serialize
    serializedObject must be(ExtendedObject(Map("a@@b" -> 1.2)).serialize)

    val deserializedObject = ExtendedBsonValue(serializedObject).deserialize[Map[String, Double]]
    deserializedObject("a.b") must be(1.2)
  }

  it should "convert objects without loosing elements" in {
    mongoDBObject.convertDateTime(BsonDocument()) must be(BsonDocument())
    mongoDBObject.convertDateTime(BsonArray()) must be(BsonArray())
    mongoDBObject.convertDateTime(BsonArray("a")) must be(BsonArray("a"))
    mongoDBObject.convertDateTime(BsonDocument("someField" -> BsonArray())) must be(
      BsonDocument("someField" -> BsonArray())
    )
  }

  it must "deserialize DateTime objects" in withMongoDBCollection("ExtendedObject") { testCollection =>
    val expectedObject = TestClassWithDate(DateTime.parse("2016-04-14T13:27:32.779Z"))

    whenCompleted(testCollection.insertOne(BsonDocument("creationDate" -> expectedObject.creationDate.toDate))) { _ =>
      whenFound[BsonValue](testCollection.find()) { docs =>
        docs must have size 1

        val doc = docs.head
        doc.isDocument must be(true)
        doc.asDocument.containsKey("creationDate") must be(true)
        ExtendedBsonValue(doc).deserialize[TestClassWithDate] must be(expectedObject)
      }
    }
  }

  it must "serialize classes with DateTime field" in {
    val dateTime = DateTime.parse("2016-04-14T13:27:32.779Z")
    val o = TestClassWithDate(dateTime)
    val expected = BsonDocument("creationDate" -> BsonDateTime(dateTime.toDate))
    ExtendedObject(o).serialize must be(expected)
  }

  it must "serialize and deserialize objects with date time" in withMongoDBCollection("ExtendedObject") {
    testCollection =>
      val expectedObject = TestClassWithDate(DateTime.parse("2016-04-14T13:27:32.779Z"))

      whenCompleted(testCollection.insertOne(ExtendedObject(expectedObject).serialize)) { _ =>
        whenFound(testCollection.find()) { docs =>
          docs must have size 1

          docs.head.isDocument must be(true)
          ExtendedBsonValue(docs.head.asDocument).deserialize[TestClassWithDate] must be(expectedObject)
        }
      }
  }

  it must "serialize and deserialize arrays" in {
    val expected = Seq("e1", "e2")
    val serialized = ExtendedObject(expected).serialize
    serialized must be(BsonArray("e1", "e2"))
    ExtendedBsonValue(serialized).deserialize[Seq[String]] must be(expected)
  }

  it must "serialize and deserialize complex objects" in {
    val expected = Parent(Child(Seq("e1", "e2")))
    val serialized = ExtendedObject(expected).serialize
    ExtendedBsonValue(serialized).deserialize[Parent] must be(expected)
  }
}

case class Parent(child: Child)
case class Child(values: Seq[String])

case class TestClassWithDate(creationDate: DateTime)
