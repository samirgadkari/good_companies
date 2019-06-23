package com.unique.companies

import scala.concurrent.{ Future, blocking, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.lang.Thread
import java.util.concurrent.TimeoutException

object async {

  def delay(n: Long): Future[Unit] = Future {
    println(s"Going to sleep for ${n / 1000} seconds")
    blocking {
      Thread.sleep(n)
    }
    println("finished sleeping")
  }

  def deltaTime[T](b: => T): Unit = {
    val begin = System.nanoTime
    try b
    catch {
      case ex: TimeoutException => println(s"Exception: ${ex.toString}")
    }
    val end = System.nanoTime
    println(s"Waited for: ${(end - begin) / 1000000L} milliseconds")
  }

  def wait(t: Duration): Unit = {
    Await.result(delay(t.toMillis), Duration.Inf)
  }

  def waitOnFuture[T](f: Future[T], t: Duration): Unit = {
    Await.ready(f, t)
  }

  def secondsToDuration(t: Int): Duration = {
    Duration(t, SECONDS)
  }
}
