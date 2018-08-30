package com.patagona.util.mongodb.dao

import java.util.Date
import java.util.concurrent.atomic.AtomicReference

import org.bson.types.ObjectId
import org.json4s.Formats
import org.json4s.JArray
import org.json4s.JField
import org.json4s.JNothing
import org.json4s.JNull
import org.json4s.JObject
import org.json4s.JValue
import org.json4s.JsonAST.JBool
import org.json4s.JsonAST.JDecimal
import org.json4s.JsonAST.JDouble
import org.json4s.JsonAST.JInt
import org.json4s.JsonAST.JString
import org.json4s.ParserUtil.ParseException
import org.json4s.mongo.JObjectParser
import org.mongodb.scala.bson.BsonArray
import org.mongodb.scala.bson.BsonBoolean
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.BsonDecimal128
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.BsonDouble
import org.mongodb.scala.bson.BsonInt32
import org.mongodb.scala.bson.BsonInt64
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.bson.BsonNumber
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.BsonRegularExpression
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.bson.BsonTimestamp
import org.mongodb.scala.bson.BsonUndefined
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.Decimal128
import org.mongodb.scala.bson.ObjectId

import collection.JavaConverters._

object JDocumentParser {

  /**
    * Set this to override JDocumentParser turning strings that are valid
    * ObjectIds into actual ObjectIds. For example, place the following in Boot.boot:
    *
    * <code>JDocumentParser.stringProcessor.default.set((s: String) => BsonString(s))</code>
    */
  val stringProcessor = new AtomicReference[String => BsonValue](defaultStringProcessor) {}

  def defaultStringProcessor(s: String): BsonValue = {
    if (ObjectId.isValid(s)) {
      BsonObjectId(new ObjectId(s))
    } else {
      BsonString(s)
    }
  }

  def serialize(a: Any)(implicit formats: Formats): JValue = serialize(a, formats)

  // scalastyle:off cyclomatic.complexity
  private def serialize(a: Any, formats: Formats): JValue = {
    a.asInstanceOf[AnyRef] match {
      case x: BsonBoolean => JBool(x.getValue)
      case x: BsonDateTime => JObject(JField("$dt", JString(formats.dateFormat.format(new Date(x.getValue)))))
      case x: BsonDecimal128 => JDecimal(x.decimal128Value().bigDecimalValue())
      case x: BsonDouble => JDouble(x.doubleValue())
      case x: BsonInt32 => JInt(x.intValue())
      case x: BsonInt64 => JInt(x.longValue())
      case x: Decimal128 => JDecimal(x.bigDecimalValue())
      case x: BsonNull => JNull
      case x: BsonObjectId => JObject(JField("$oid", JString(x.getValue.toHexString)))
      case x: BsonRegularExpression =>
        JObject(JField("$regex", JString(x.getPattern)) :: JField("$flags", JString(x.getOptions)) :: Nil)
      case x: BsonString => JString(x.getValue)
      case x: BsonTimestamp => JObject(JField("$dt", JString(formats.dateFormat.format(new Date(x.getValue)))))
      case x: BsonUndefined => JNothing
      case x: ObjectId => JObject(JField("$oid", JString(x.toHexString)))
      case x: BsonArray => JArray(x.getValues.asScala.toList.map(x => serialize(x, formats)))
      case x: BsonDocument =>
        JObject(
          x.entrySet().asScala.toList.map { entry =>
            JField(entry.getKey, serialize(entry.getValue, formats))
          }
        )
      case x: BsonValue => throw new RuntimeException(s"BSON type ${x.getBsonType.name()} -> JValue is not supported")
      case x => JObjectParser.serialize(x)(formats)
    }
  }
  // scalastyle:on cyclomatic.complexity

  def parse(jo: JValue)(implicit formats: Formats): BsonValue =
    Parser.parse(jo, formats)

  object Parser {
    def parse(jv: JValue, formats: Formats): BsonValue = jv match {
      case jo: JObject =>
        parseObject(jo.obj, formats)
      case ja: JArray =>
        parseArray(ja.arr, formats)
      case x => throw new ParseException(s"Couldn't parse $x to a Document", null) // scalastyle:ignore null
    }

    private def parseJValue(value: JValue, formats: Formats): BsonValue = value match {
      case JObject(JField("$oid", JString(s)) :: Nil) if ObjectId.isValid(s) =>
        BsonObjectId(new ObjectId(s))
      case JObject(JField("$regex", JString(s)) :: JField("$flags", JInt(f)) :: Nil) =>
        throw new RuntimeException("JValue $regex -> BsonValue needs to have string flags, not integer flags.")
      case JObject(JField("$regex", JString(s)) :: JField("$flags", JString(f)) :: Nil) =>
        BsonRegularExpression(s, f)
      case JObject(JField("$dt", JString(s)) :: Nil) =>
        formats.dateFormat
          .parse(s)
          .map { d =>
            BsonDateTime(d)
          }
          .getOrElse(throw new RuntimeException(s"JSon $$dt -> BsonDateTime conversion failed for $s"))
      case JObject(JField("$uuid", JString(_)) :: Nil) =>
        throw new RuntimeException("JValue $uuid -> BsonValue parsing not implemented")
      case JArray(jarr) => parseArray(jarr, formats)
      case JObject(jo) => parseObject(jo, formats)
      case jv: JValue => renderValue(jv, formats)
    }

    private def parseArray(arr: List[JValue], formats: Formats): BsonArray = {
      BsonArray(trimArr(arr).map(value => parseJValue(value, formats)))
    }

    private def parseObject(obj: List[JField], formats: Formats): BsonDocument = {
      val converted = trimObj(obj).map {
        case (key, value) =>
          key -> parseJValue(value, formats)
      }

      BsonDocument(converted)
    }

    private def renderValue(jv: JValue, formats: Formats): BsonValue = jv match {
      case JBool(b) => BsonBoolean(java.lang.Boolean.valueOf(b))
      case JInt(n) => renderInteger(n)
      case JDouble(n) => BsonNumber(n)
      case JNull => BsonNull()
      case JNothing => throw new RuntimeException("JNothing -> BsonValue not possible")
      case JString(null) => BsonString("null") // scalastyle:ignore null
      case JString(s) => stringProcessor.get()(s)
      case _ => BsonString("")
    }

    // FIXME: This is not ideal.
    private def renderInteger(i: BigInt): BsonValue = {
      if (i <= java.lang.Integer.MAX_VALUE && i >= java.lang.Integer.MIN_VALUE) {
        BsonNumber(i.intValue)
      } else if (i <= java.lang.Long.MAX_VALUE && i >= java.lang.Long.MIN_VALUE) {
        BsonNumber(i.longValue)
      } else {
        throw new RuntimeException(s"JValue integer -> BsonValue out of range: ${i.toString()}")
      }
    }

    private def trimArr(xs: List[JValue]) = xs.filterNot(_ == JNothing)
    private def trimObj(xs: List[JField]) = xs.filterNot(_._2 == JNothing)
  }
}
