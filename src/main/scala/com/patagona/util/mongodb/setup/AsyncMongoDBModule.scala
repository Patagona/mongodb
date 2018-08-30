package com.patagona.util.mongodb.setup

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoDatabase

class AsyncMongoDBModule(val config: MongoDBConfig) extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    val client = MongoDBConfig.createAsyncClient(config)
    bind[MongoClient].toInstance(client)
    bind[MongoDatabase].toInstance(client.getDatabase(config.db))
  }
}
