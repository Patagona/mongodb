package com.patagona.util.mongodb.dao

import org.joda.time.DateTime
import org.json4s._
import org.json4s.JsonAST.JValue
import org.mongodb.scala.bson.BsonArray
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.BsonDocument

import collection.JavaConverters._

case class ExtendedBsonValue(bsonValue: BsonValue) {
  def deserialize[A](
    implicit manifest: Manifest[A],
    formats: Formats = MongoFormats.baseFormatsWithMongoDateTime
  ): A = {
    toJValue.extract[A](formats, manifest)
  }

  def toJValue: JValue = {
    val converted = convertDateTime(bsonValue)
    unescape(JDocumentParser.serialize(converted)(MongoFormats.baseFormats))
  }

  def unescape(json: JValue): JValue = json.mapField {
    case (key, value) if key.contains("@@") =>
      key.replace("@@", ".") -> unescape(value)
    case (key, value) => key -> value
  }

  def convertDateTime: Any => BsonValue = {
    case v: DateTime => BsonDocument("$dt" -> v.toString())
    case a: BsonArray => BsonArray(a.asScala.map(convertDateTime))
    case o: BsonDocument => BsonDocument(o.asScala.mapValues(convertDateTime).toSeq)
    case r: BsonValue => r
    case unsupported =>
      throw new IllegalArgumentException(s"convertDateTime cannot convert type ${unsupported.getClass.getSimpleName}")
  }
}

case class InvalidMongoDBObjectException(message: String) extends Exception(message)
