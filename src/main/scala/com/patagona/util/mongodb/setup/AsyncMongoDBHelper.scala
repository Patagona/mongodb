package com.patagona.util.mongodb.setup

import org.mongodb.scala.MongoDatabase

trait AsyncMongoDBHelper {
  def withMongoDB(config: MongoDBConfig)(f: MongoDatabase => Unit): Unit = {
    val client = MongoDBConfig.createAsyncClient(config)
    val db: MongoDatabase = client.getDatabase(config.db)
    try {
      f(db)
    } finally {
      client.close()
    }
  }
}
