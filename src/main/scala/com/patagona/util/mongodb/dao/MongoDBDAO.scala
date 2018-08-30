package com.patagona.util.mongodb.dao

import java.util.UUID

import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.SingleObservable
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._

trait MongoDBDAO {
  def db: MongoDatabase

  def generateUniqueId(
    collection: String,
    idGenerator: () => String = () => { UUID.randomUUID().toString }
  ): SingleObservable[String] = {
    val token = idGenerator()
    db.getCollection(collection).countDocuments(equal("_id", token)).flatMap {
      case 0 => SingleObservable.apply(token)
      case _ => generateUniqueId(collection, idGenerator)
    }
  }

  implicit def extendMongoDBObject(o: BsonValue): ExtendedBsonValue = ExtendedBsonValue(o)

  implicit def extendedObject(o: Any): ExtendedObject = ExtendedObject(o)

  def safeSet(fields: (String, Any)*): Bson = {
    val cleanedFields = fields.filter {
      case (_, null) => false // scalastyle:ignore
      case (_, None) => false
      case _ => true
    }

    combine(cleanedFields.map {
      case (field, value) => set(field, value)
    }: _*)
  }
}
