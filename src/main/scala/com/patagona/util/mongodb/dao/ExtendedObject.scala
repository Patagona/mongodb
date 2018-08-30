package com.patagona.util.mongodb.dao

import org.json4s.JsonAST.JValue
import org.json4s.{Extraction, Formats}
import org.mongodb.scala.bson.BsonValue

case class ExtendedObject(obj: Any) {
  def serialize(implicit formats: Formats = MongoFormats.baseFormatsWithMongoDateTime): BsonValue = {
    val json = Extraction.decompose(obj)
    JDocumentParser.parse(escape(json))
  }

  def escape(json: JValue): JValue = json.mapField {
    case (key, value) if key.contains(".") =>
      key.replace(".", "@@") -> escape(value)
    case (key, value) => key -> value
  }
}
