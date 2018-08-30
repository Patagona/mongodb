package com.patagona.util.mongodb

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import org.mongodb.scala.MongoDatabase

class MongoDBTestModule(val db: MongoDatabase) extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[MongoDatabase].toInstance(db)
  }
}
