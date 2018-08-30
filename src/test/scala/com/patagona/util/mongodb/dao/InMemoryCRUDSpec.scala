package com.patagona.util.mongodb.dao

import org.mongodb.scala.bson.BsonDocument
import org.scalacheck.Gen
import org.scalatest.MustMatchers
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks

import scala.concurrent.Future
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

class InMemoryCRUDSpec extends WordSpec with MustMatchers with PropertyChecks with ScalaFutures {
  "InMemoryCRUD" when {
    "reading many values" should {
      "return the expected documents" in {
        val crud = new CRUD with InMemoryCRUD
        implicit val dBContext = DBContext(null, "", "1") // scalastyle:ignore null

        forAll(Gen.chooseNum[Int](1, 100)) { max: Int =>
          val values = (0 until max).map(_.toString)
          val futures = values.map { value =>
            val groundTruth = Seq(Map("key" -> value) -> value)
            crud
              .upsert(Map("key" -> value))(value)(_ => BsonDocument("data" -> value))
              .flatMap { _ =>
                crud.readMany(Map("key" -> Seq(value)), 0, values.size) {
                  _.asDocument().get("data").asString().getValue
                }
              }
              .map { result =>
                groundTruth -> result
              }
          }

          whenReady(Future.sequence(futures), timeout(1 seconds)) { results =>
            results.map {
              case (groundTruth, result) =>
                result must be(groundTruth)
            }
          }
        }
      }
    }

    "counting many values" should {
      "return the expected document count" in {
        val crud = new CRUD with InMemoryCRUD
        implicit val dBContext = DBContext(null, "", "1") // scalastyle:ignore null

        forAll(Gen.chooseNum[Int](1, 100)) { max: Int =>
          val values = (0 until max).map(_.toString)

          val futures = values.map { value =>
            val groundTruth = Seq(Map("key" -> value) -> value).size
            crud
              .upsert(Map("key" -> value))(value)(_ => BsonDocument("data" -> value))
              .flatMap { _ =>
                crud.countMany(Map("key" -> Seq(value)))
              }
              .map { count =>
                groundTruth -> count
              }
          }

          whenReady(Future.sequence(futures), timeout(1 seconds)) { results =>
            results.map {
              case (groundTruth, result) =>
                result must be(groundTruth)
            }
          }
        }
      }
    }
  }
}
