package com.unique.companies

object Config {

  type Cfg = Map[String, String]

  def toTuple(s: Array[String]) = (s: @unchecked) match {
    case Array(s1, s2) => (s1, s2)
    case Array(s1)     => (s1, "")
  }

  def load(): Cfg = {

    import scala.io.Source

    val confStream = getClass.getResourceAsStream("/companies.conf")
    val lines = Source.fromInputStream(confStream).getLines().toList

    Map[String, String](
      {
        for (line <- lines) yield {
          val (key, value) = toTuple(line split ("=") map (_.trim))
          (key, value)
        }
      }: _*
    )
  }
}

