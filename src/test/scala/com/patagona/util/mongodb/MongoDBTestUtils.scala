package com.patagona.util.mongodb

import org.mongodb.scala.Completed
import org.mongodb.scala.FindObservable
import org.mongodb.scala.SingleObservable
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait MongoDBTestUtils extends ScalaFutures {
  def whenCompleted(observable: SingleObservable[Completed], timeoutDuration: FiniteDuration = 10 seconds)(
    f: Completed => Unit
  )(implicit ec: ExecutionContext): Unit = {
    val future = observable.toFuture.recover {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }

    whenReady(future, timeout(timeoutDuration)) { completed =>
      f(completed)
    }
  }

  def whenFound[A](observable: FindObservable[A], timeoutDuration: FiniteDuration = 10 seconds)(
    f: Seq[A] => Unit
  )(implicit ec: ExecutionContext): Unit = {
    val future = observable.toFuture.recover {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }

    whenReady(future, timeout(timeoutDuration)) { result =>
      f(result)
    }
  }
}
