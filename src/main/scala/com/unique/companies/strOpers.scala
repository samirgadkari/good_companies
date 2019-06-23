package com.unique.companies

object StringUtil {

  def num(s: String): Option[Double] = {
    val numRegex = """^\s*\$*\s*([\-\+]*)\s*\(?\s*(\s*[0-9,\.Ee]+)\s*\)?\s*$""".r
    val n = s.trim.replaceAll(" ", "").replaceAll(",", "").replaceAll("'", "")
    if (n.length == 0) None
    else if (n == ".") None
    else if (n.toLowerCase == "e.") None
    else {
      n match {
        case numRegex(sign, number) =>
          try {
            if ((s contains '(') && (s contains ')')) Some(("-" ++ number).toDouble)
            else Some(("+" ++ number).toDouble)
          } catch {
            case ex: Exception =>
              println(s"Error getting number from s: $s sign: $sign number: $number")
              None
          }
        case _ => None
      }
    }
  }

  def isNum(s: String): Boolean = {
    num(s) match {
      case Some(x) => true
      case None    => false
    }
  }

  def stringToTuple(s: String, c: Char): (String, String) =
    s split (c) match {
      case Array(s1, s2) => (s1, s2)
      case _ => throw new IllegalArgumentException(
        s"String $s cannot be split into two strings"
      )
    }

  def optionToString(s: Option[String]): String = s match {
    case None => ""
    case Some(str) =>
      num(str) match {
        case None    => str
        case Some(d) => ""
      }
  }

  def strToFiscalYrEnd(s1: Option[String]): Option[Docs.FiscalYrEnd] = {
    s1 match {
      case Some(s) =>
        s split ("-") match {
          case Array(m, d) => Some(Docs.FiscalYrEnd(m, d))
          case _           => None
        }
      case None =>
        None
    }
  }

  import java.time.{ LocalDate }
  def strToLocalDate(s: Option[String]): Option[LocalDate] = {

    import java.time.format.{ DateTimeParseException, DateTimeFormatter }

    def convertToNum(str: String): String = {
      // println(s">>> $str")

      val moDayYr = """^([a-zA-Z]*)\.?\s*[-]?\s*([0-9]*)\s*[-]?\s*[,]?\s*([0-9]*)$""".r
      val dayMoYr = """^([0-9]*)\s*[-]?\s*([a-zA-Z]*)\.?\s*[-]?\s*[,]?\s*?\s*([0-9]*)$""".r

      str match {
        case moDayYr(month, day, year) =>
          // println(s"1 >>> $month $day $year")
          if (day.length == 0) str
          else
            "%02d-%3s-%s".format(day.toInt, month.trim.replace('.', ' '), year)
        case dayMoYr(day, month, year) =>
          // println(s"3 >>> $day $month $year")
          if (day.length == 0) str
          else
            "%02d-%3s-%s".format(day.toInt, month.trim.replace('.', ' '), year)
        case _ =>
          // println(s"4 >>> $s")
          str
      }
    }

    s match {
      case Some(d1) =>
        // println(s"$d1")
        val d = convertToNum(d1 split ('\n') map (_.trim.filter(_ > ' ')) mkString)
        // println(s"date value: $d")

        try {
          Some(LocalDate.parse(d))
        } catch {
          case ex: DateTimeParseException =>
            // println(s"DateTimeParseException for $d")

            val formats = List[String]("dd-MMM-yyyy", "dd-MMM-yy")

            val dates = for (format <- formats) yield {
              // println(s"d: $d format: $format")
              try {
                val f = DateTimeFormatter.ofPattern(format)
                Some(LocalDate.parse(d, f))
              } catch {
                case ex: Exception =>
                  // println(s"exception name: $ex")
                  None
              }
            }
            val validDates = dates filter (_ match {
              case Some(x) => true
              case None    => false
            })

            if (validDates.length == 0) {
              println(s"Cannot parse date: $d")
              None
            } else validDates.head
        }
      case None => None
    }
  }
}

