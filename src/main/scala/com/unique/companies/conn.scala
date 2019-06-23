package com.unique.companies

import org.jsoup._
import org.jsoup.Connection._
import scala.collection.JavaConverters._
import java.net.SocketTimeoutException

object Conn {

  val userAgent = "Mozilla/5.0"

  def connect(url: String, method: Method,
              data: Map[String, String], followRedirects: Boolean): Option[Response] = {

    val timesLooped = 0
    val numRetries = 5
    val timeoutInSeconds: Int = 1

    def connectOnce(timeout: Int): Response = {

      Jsoup.connect(url)
        .userAgent(userAgent)
        .timeout(timeoutInSeconds * 1000) // convert to milliseconds
        .method(method)
        .data(data.asJava)
        .ignoreContentType(true)
        .followRedirects(followRedirects)
        .execute()
    }

    def conn(timesLooped: Int, timeout: Int): Option[Response] = {

      try {
        Some(connectOnce(timeout))
      } catch {
        case e @ (_: org.jsoup.HttpStatusException | _: SocketTimeoutException) =>
          println(s"Exception occurred: $e")
          if (timesLooped < numRetries) {
            async.wait(async.secondsToDuration(timeout))
            conn(timesLooped + 1, timeout * 2) // backoff double the timeout earlier
          } else {
            println("Connection retries exceeded !!!")
            None
          }
        case ex: Throwable =>
          throw new Exception(s"Unrecognized exception: $ex - not handled !!!")
      }
    }

    conn(0, timeoutInSeconds)
  }
}
