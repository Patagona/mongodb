package com.patagona.util.mongodb

import org.mongodb.scala.Completed
import org.mongodb.scala.FindObservable
import org.mongodb.scala.SingleObservable
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

trait MongoDBTestUtils extends ScalaFutures {
  def whenCompleted(observable: SingleObservable[Completed], timeoutDuration: FiniteDuration = 10 seconds)(
    f: Completed => Unit
  ): Unit = {
    whenReady(observable.toFuture, timeout(timeoutDuration)) { completed =>
      f(completed)
    }
  }

  def whenFound[A](observable: FindObservable[A], timeoutDuration: FiniteDuration = 10 seconds)(
    f: Seq[A] => Unit
  ): Unit = {
    whenReady(observable.toFuture, timeout(timeoutDuration)) { result =>
      f(result)
    }
  }
}
