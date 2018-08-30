package com.patagona.util.mongodb.dao

import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.JsonAST.{JField, JObject, JString, JValue}
import org.json4s.reflect.TypeInfo
import org.json4s.{Formats, MappingException, Serializer}

case class MongoDateTimeSerializer(fieldName: String = "$dt") extends Serializer[DateTime] {
  private val DateClass = classOf[DateTime]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), DateTime] = {
    case (TypeInfo(DateClass, _), json) =>
      json match {
        case JObject(JField(`fieldName`, JString(s)) :: Nil) =>
          DateTime.parse(s.toString).withZone(DateTimeZone.UTC)
        case JString(s) => DateTime.parse(s)
        case x: Any => throw new MappingException(s"Can't convert $x to DateTime")
      }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case d: DateTime =>
      JObject(JField(fieldName, JString(d.toString)) :: Nil)
  }
}
