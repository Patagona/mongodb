package com.patagona.util.mongodb

import org.mongodb.scala.{FindObservable, Observable, SingleObservable}
import org.scalatest.concurrent.ScalaFutures
import shapeless.=:!=

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait MongoDBTestUtils extends ScalaFutures {

  def whenFindCompleted[A, B](observable: FindObservable[A], timeoutDuration: FiniteDuration = 10 seconds)(
    f: Seq[A] => B
  )(implicit ec: ExecutionContext): B = whenCompleted[A, B, FindObservable[A]](observable, timeoutDuration)(f)

  private def whenCompleted[A, B, C <: Observable[A]](observable: C, timeoutDuration: FiniteDuration)(
    f: Seq[A] => B
  )(implicit ec: ExecutionContext, ev: C =:!= SingleObservable[A]): B = {
    val future = observable.toFuture.recover {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }

    whenReady(future, timeout(timeoutDuration)) { completed =>
      f(completed)
    }
  }

  def whenSingleCompleted[A, B](observable: SingleObservable[A], timeoutDuration: FiniteDuration = 10 seconds)(
    f: A => B
  )(implicit ec: ExecutionContext): B = {
    val future = observable.toFuture.recover {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }

    whenReady(future, timeout(timeoutDuration)) { completed =>
      f(completed)
    }
  }
}
