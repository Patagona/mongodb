package com.patagona.util.mongodb.config

import com.mongodb.ServerAddress
import com.patagona.util.mongodb.setup.MongoDBConfig
import org.scalatest.{AsyncWordSpec, MustMatchers}

class MongoDBConfigSpec extends AsyncWordSpec with MustMatchers {
  "MongoDBConfig" should {
    "create non-replicaset connections" in {
      val client = MongoDBConfig.createAsyncClient(
        MongoDBConfig(
          "localhost",
          27017,
          "test",
          None
        )
      )

      client.settings.getClusterSettings.getHosts must contain(new ServerAddress("localhost", 27017))
    }

    "deny creating non-replicaset connections with multiple hosts" in {
      a[IllegalArgumentException] must be thrownBy MongoDBConfig.createAsyncClient(
        MongoDBConfig(
          "localhost,127.0.0.1",
          27017,
          "test",
          None
        )
      )
    }

    "create replicaset connections with single hosts" in {
      val client = MongoDBConfig.createAsyncClient(
        MongoDBConfig(
          "localhost",
          27017,
          "test",
          Some("rs0")
        )
      )

      client.settings.getClusterSettings.getHosts must contain(new ServerAddress("localhost", 27017))
      client.settings.getClusterSettings.getRequiredReplicaSetName must be("rs0")
    }

    "create replicaset connections with multiple hosts" in {
      val client = MongoDBConfig.createAsyncClient(
        MongoDBConfig(
          "localhost , 127.0.0.1",
          27017,
          "test",
          Some("rs0")
        )
      )

      client.settings.getClusterSettings.getHosts must contain allElementsOf Seq(
        new ServerAddress("localhost", 27017),
        new ServerAddress("127.0.0.1", 27017)
      )
      client.settings.getClusterSettings.getRequiredReplicaSetName must be("rs0")
    }
  }
}
