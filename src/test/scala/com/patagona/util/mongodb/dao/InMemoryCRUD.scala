package com.patagona.util.mongodb.dao

import org.mongodb.scala.ObservableImplicits
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.BsonValue

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait InMemoryCRUD extends ObservableImplicits {
  self: CRUD =>

  var store: Map[Map[String, String], (String, BsonDocument)] = Map()

  override def upsert[A <: AnyRef](
    keys: Map[String, String]
  )(data: A)(conversion: A => BsonValue)(implicit context: DBContext, ec: ExecutionContext): Future[A] = {
    require(keys.nonEmpty, "Keys must not be empty when updating a document")
    store = store.updated(keys, context.schemaVersion -> BsonDocument("data" -> conversion(data)))
    Future.successful(data)
  }

  override def read[A <: AnyRef](
    keys: Map[String, String]
  )(
    conversion: BsonValue => A
  )(implicit context: DBContext, ec: ExecutionContext): Future[Option[(Map[String, String], A)]] = {
    val result = store.get(keys).map {
      case (schemaVersion, document) =>
        require(schemaVersion == context.schemaVersion)
        keys -> extractData(conversion)(document)
    }
    Future.successful(result)
  }

  override def readMany[A <: AnyRef](keys: Map[String, Seq[String]], start: Int, limit: Int)(
    conversion: BsonValue => A
  )(implicit context: DBContext, ec: ExecutionContext): Future[Seq[(Map[String, String], A)]] = {
    val result = store
      .filterKeys { currentKeys =>
        keys.size == currentKeys.size && currentKeys.forall { case (key, value) => contains(keys, key, value) }
      }
      .toSeq
      .map {
        case (key, (schemaVersion, document)) =>
          require(schemaVersion == context.schemaVersion)
          key -> extractData(conversion)(document)
      }
    val totalSize = result.size
    Future.successful(result.takeRight(totalSize - start).take(limit))
  }

  override def countMany(
    keys: Map[String, Seq[String]]
  )(implicit context: DBContext, ec: ExecutionContext): Future[Long] = {
    val count = store
      .filterKeys { currentKeys =>
        keys.size == currentKeys.size && currentKeys.forall { case (key, value) => contains(keys, key, value) }
      }
      .count { case (_, (schemaVersion, _)) => schemaVersion == context.schemaVersion }

    Future.successful(count)
  }

  def contains(set: Map[String, Seq[String]], key: String, value: String): Boolean = {
    set.get(key).exists(_.contains(value))
  }
}
