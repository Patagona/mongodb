package com.patagona.util.mongodb.dao

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import com.patagona.util.mongodb.exceptions.MongoDBUpsertFailedException
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.bson.BsonArray
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.model.UpdateOptions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait CRUD {
  def prepareDocument[A](
    keys: Map[String, String],
    schemaVersion: String,
    data: A,
    conversion: A => BsonValue
  ): Document = {
    val document = buildQuery(keys)
    val now = DateTime.now(DateTimeZone.UTC).toString()

    val actualData = data match {
      case s: String => BsonString(s)
      case _ => conversion(data)
    }

    document + (
      "schemaVersion" -> schemaVersion,
      "updateDate" -> now,
      "data" -> actualData
    )
  }

  def upsert[A <: AnyRef](
    keys: Map[String, String]
  )(data: A)(conversion: A => BsonValue)(implicit context: DBContext, ec: ExecutionContext): Future[A] = {
    require(keys.nonEmpty, "Keys must not be empty when updating a document")
    require(!keys.contains("id"), "Internal mongodb identifier is not allowed as key")
    val query = buildQuery(keys)
    val document = prepareDocument(keys, context.schemaVersion, data, conversion)
    val upsertParameters = Document(
      "$set" -> document,
      "$setOnInsert" -> Document(
        "creationDate" -> Option(document.getString("updateDate")).getOrElse(
          throw new RuntimeException(s"Could not insert document: No updateDate was generated in ${document.toString}")
        )
      )
    )

    context.collection.updateOne(query, upsertParameters, UpdateOptions().upsert(true)).toFuture.map { result =>
      if (Option(result.getUpsertedId).isEmpty && result.getModifiedCount == 0) {
        throw new MongoDBUpsertFailedException(s"Upsert of document with keys $keys resulted in 0 changes.")
      }

      data
    }
  }

  def escapeKey(key: String): String = {
    require(!key.isEmpty, "Key must not be empty")
    "_" + key
  }

  def unescapeKey(escapedkey: String): String = {
    if (escapedkey.startsWith("_")) {
      escapedkey.substring(1)
    } else {
      throw new RuntimeException(s"Key $escapedkey does not start with '_'")
    }
  }

  def buildQuery(keys: Map[String, String]): Document = {
    Document(keys.toList.map {
      case (k, v) =>
        require(v.nonEmpty, s"Query value for key $k must not be empty")
        escapeKey(k) -> v
    })
  }

  def read[A <: AnyRef](
    keys: Map[String, String]
  )(
    conversion: BsonValue => A
  )(implicit context: DBContext, ec: ExecutionContext): Future[Option[(Map[String, String], A)]] = {
    val query = buildQuery(keys)
    context.collection
      .find(query)
      .map { document =>
        verifySchemaVersion(context.schemaVersion, document)
        extractKeys(document.toBsonDocument) -> extractData(conversion)(document)
      }
      .headOption()
  }

  def extractData[A <: AnyRef](conversion: BsonValue => A)(document: Document): A = {
    document.get("data") match {
      case None =>
        throw new RuntimeException(s"Could not find data field in document $document")
      case Some(s: BsonString) =>
        s.getValue.asInstanceOf[A]
      case Some(o: BsonDocument) =>
        conversion(o)
      case Some(o: BsonArray) =>
        conversion(o)
      case _ =>
        throw new RuntimeException(s"Unknown data type in document $document")
    }
  }

  private def verifySchemaVersion(schemaVersion: String, document: Document): Unit = {
    val actualVersion = document
      .get("schemaVersion")
      .getOrElse(
        throw new RuntimeException(s"Invalid schema version. Expected $schemaVersion but found no schemaVersion field.")
      )

    if (!actualVersion.isString || actualVersion.asString().getValue != schemaVersion) {
      throw new RuntimeException(s"Invalid schema version. Expected $schemaVersion but found $actualVersion")
    }
  }

  def extractMatchedValues(keys: Set[String])(doc: BsonDocument): Map[String, String] = {
    keys.map(key => key -> doc.getString(key).getValue).toMap
  }

  def transformKey[A, B](f: A => A)(v: (A, B)): (A, B) = f(v._1) -> v._2

  def countMany(keys: Map[String, Seq[String]])(implicit context: DBContext, ec: ExecutionContext): Future[Long] = {
    val escapedKeys = keys.map { transformKey(escapeKey) }
    val fields = escapedKeys.map { case (key, values) => in(key, values: _*) }.toSeq
    val query = if (fields.size > 1) {
      context.collection.countDocuments(and(fields: _*))
    } else if (fields.size == 1) {
      context.collection.countDocuments(fields.head)
    } else {
      context.collection.countDocuments()
    }

    query.toFuture
  }

  def readMany[A <: AnyRef](
    keys: Map[String, Seq[String]],
    start: Int,
    limit: Int,
    sortBy: Option[(String, Int)] = None
  )(
    conversion: BsonValue => A
  )(implicit context: DBContext, ec: ExecutionContext): Future[Seq[(Map[String, String], A)]] = {
    val escapedKeys = keys.map { transformKey(escapeKey) }
    val fields = escapedKeys.map { case (key, values) => in(key, values: _*) }.toSeq
    val query = and(fields: _*)
    val totalResult = context.collection.find(query)

    val sortedResult = sortBy match {
      case None => totalResult
      case Some((sortByField, sortOrder)) => totalResult.sort(BsonDocument(sortByField -> sortOrder))
    }

    sortedResult
      .skip(start)
      .limit(limit)
      .map { doc =>
        verifySchemaVersion(context.schemaVersion, doc)
        extractKeys(doc.toBsonDocument) -> extractData(conversion)(doc)
      }
      .toFuture
  }

  def extractKeys(doc: BsonDocument): Map[String, String] = {
    val keys = doc.keys.toSet.filter(_.startsWith("_"))
    val cleanedKeys = keys.filterNot(_ == "_id")
    extractMatchedValues(cleanedKeys)(doc).map(transformKey(unescapeKey))
  }

}

case class DBContext(db: MongoDatabase, collectionName: String, schemaVersion: String) {
  lazy val collection: MongoCollection[Document] = db.getCollection(collectionName)
}
