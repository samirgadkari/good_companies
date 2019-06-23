package com.unique.companies

import java.time.{ LocalDate, YearMonth }
import scala.util.matching.Regex

object DocInfo {

  def apply(
    t:               Option[String],
    periodEndDate:   Option[LocalDate],
    entityRegName:   Option[String],
    fiscalYrEndDate: Option[Docs.FiscalYrEnd]
  ): DocInfo =
    new DocInfo(t, periodEndDate,
                entityRegName, fiscalYrEndDate)

  def apply(that: DocInfo): DocInfo =
    new DocInfo(that.typ, that.periodEndDate,
                that.entityRegName, that.fiscalYrEndDate)

  def apply[T](items: List[Item]): DocInfo = {

    val reType = """(?i)^(?:document\s*type)|(?:sec\s*form)$""".r
    val reDocPeriodEndDate = """(?i)^(?:document)?\s*period\s*end\s*date$""".r
    val reEntityRegName = """(?i)^(?:entity)?\s*registrant\s*name$""".r
    val reFiscalYrEndDate = """(?i)^(?:current)?\s*fiscal\s*year\s*end\s*(?:date)?$""".r

    def date(str: Option[String]): Option[LocalDate] = str match {
      case Some(s) =>
        s count (_ == '-') match {
          case 2 => Some(LocalDate parse (s + "T23:59:59Z"))
          case 1 => None
          case _ => None
        }
      case None => None
    }

    def stripExtraPrefixes(s: String): String =
      s stripPrefix ("'") stripPrefix ("--")

    def find(s: String, r: Regex, cur: Item): Option[String] = {

      val m = r.findFirstIn(s)
      m match {
        case Some(x: String) =>

          val strValue = stripExtraPrefixes(cur.cellTitle)
          Some(strValue)

        case None =>
          None
      }
    }

    def isNone(di: Option[Any]): Boolean = di match {
      case None => true
      case _    => false
    }

    def updateDIWithItems(
      items: List[Item],
      m:     DocInfo
    ): DocInfo = {

      if (items.length == 0) m
      else {
        val cur = items.head
        val m2 = updateDIWithRowTitles(items.head.rowTitles, m, cur)

        val t = items.tail
        val m3 = if (t.length > 0) {
          updateDIWithItems(items.tail, m2)
        } else m2

        m3
      }
    }

    def updateDI(
      m: DocInfo,
      s: String,
      v: Option[String]
    ): DocInfo = {

      v match {
        case None => m
        case _ =>
          val typ = m.typ
          val periodEndDate = m.periodEndDate
          val entityRegName = m.entityRegName
          val fiscalYrEndDate = m.fiscalYrEndDate
          s match {
            case "typ" =>
              typ match {
                case Some(x) => m
                case None    => DocInfo(v, periodEndDate, entityRegName, fiscalYrEndDate)
              }
            case "periodEndDate" =>
              periodEndDate match {
                case Some(x) => m
                case None =>
                  DocInfo(typ, StringUtil.strToLocalDate(v),
                          entityRegName, fiscalYrEndDate)
              }
            case "entityRegName" =>
              entityRegName match {
                case Some(x) => m
                case None    => DocInfo(typ, periodEndDate, v, fiscalYrEndDate)
              }
            case "fiscalYrEndDate" =>
              fiscalYrEndDate match {
                case Some(x) => m
                case None => DocInfo(typ, periodEndDate, entityRegName,
                                     StringUtil.strToFiscalYrEnd(v))
              }
          }
      }
    }

    def updateDIWithRowTitles(
      rts: List[String],
      m:   DocInfo,
      cur: Item
    ): DocInfo = {

      if (rts.length == 0) m
      else {
        val rt = rts.head
        val m1 = updateDI(m, "typ", find(rt, reType, cur))
        val m2: DocInfo =
          updateDI(m1, "periodEndDate",
                   find(rt, reDocPeriodEndDate, cur))
        val m3: DocInfo =
          updateDI(m2, "entityRegName",
                   find(rt, reEntityRegName, cur))
        val m4: DocInfo =
          updateDI(m3, "fiscalYrEndDate",
                   find(rt, reFiscalYrEndDate, cur))

        val t = rts.tail
        if (t.length > 0) updateDIWithRowTitles(t, m4, cur)
        else m4
      }
    }

    val di = updateDIWithItems(items, DocInfo(None, None, None, None))
    if (di == DocInfo(None, None, None, None))
      throw new UnsupportedOperationException(
        "Cannot have document info be None"
      )

    di
  }
}

class DocInfo(
  val typ:             Option[String],
  val periodEndDate:   Option[LocalDate],
  val entityRegName:   Option[String],
  val fiscalYrEndDate: Option[Docs.FiscalYrEnd]
) {
  override def toString() =
    "DocInfo: typ: %10s periodEndDate: %s entityRegName: %s fiscalYrEndDate: %s".format(
      typ, periodEndDate, entityRegName, fiscalYrEndDate
    )
}

