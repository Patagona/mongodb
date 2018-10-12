package com.patagona.util.mongodb.exceptions

import com.mongodb.MongoWriteException

object MongoDBExceptions {
  case class DuplicateKeyException(cause: MongoWriteException) extends Exception(cause.getMessage, cause) {
    require(cause.getError.getCode == 11000)
  }

  case class UpsertFailedException(message: String = "Upsert failed with unknown reason") extends Exception(message)
}
