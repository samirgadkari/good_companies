package com.unique.companies

import java.time.LocalDate
import scala.collection.JavaConverters._

abstract class Result(key: String, value: Any) extends Product with Serializable

case class ID(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class CentralIndexKey(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class AccessorNumber(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class SheetName(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class DocumentType(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class DocPeriodEndDate(k: String, v: LocalDate) extends Result(k, v) {
  override def toString = v.toString
}
case class EntityRegistrantName(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class FiscalYearEndDate(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class ContentType(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class CellText(k: String, v: String) extends Result(k, v) {
  override def toString = v.toString
}
case class CellValue(k: String, v: Double) extends Result(k, v) {
  override def toString = v.toString
}
case class RowTitles(k: String, v: List[String]) extends Result(k, v) {
  override def toString =
    "[" + (for (x <- v) x + ", ") + "]"
}
case class ColTitles(k: String, v: List[String]) extends Result(k, v) {
  override def toString =
    "[" + (for (x <- v) x + ", ") + "]"
}
case class CellRow(k: String, v: Int) extends Result(k, v) {
  override def toString = v.toString
}
case class CellCol(k: String, v: Int) extends Result(k, v) {
  override def toString = v.toString
}
case class Version(k: String, v: java.lang.Long) extends Result(k, v) {
  override def toString = v.toString
}

object Result {
  def apply(key: String, value: Any) = key match {

    case "_version_"     => new Version(key, value.asInstanceOf[java.lang.Long])
    case "id"            => new ID(key, value.asInstanceOf[String])
    case "cik"           => new CentralIndexKey(key, value.asInstanceOf[String])
    case "accessor_num"  => new AccessorNumber(key, value.asInstanceOf[String])
    case "sheet_name"    => new SheetName(key, value.asInstanceOf[String])
    case "document_type" => new DocumentType(key, value.asInstanceOf[String])
    case "document_period_end_date" =>
      import java.time.ZoneId
      val date = value.asInstanceOf[java.util.Date]
      new DocPeriodEndDate(key, date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
    case "entity_registrant_name"       => new EntityRegistrantName(key, value.asInstanceOf[String])
    case "current_fiscal_year_end_date" => new FiscalYearEndDate(key, value.asInstanceOf[String])
    case "content_type"                 => new ContentType(key, value.asInstanceOf[String])

    case "cell_text"                    => new CellText(key, value.asInstanceOf[String])
    case "cell_value"                   => new CellValue(key, value.asInstanceOf[Double])
    case "row_titles" =>
      val values = value.asInstanceOf[java.util.ArrayList[String]]
      val scalaValues = values.asScala
      new RowTitles(key, scalaValues.toList)
    case "col_titles" =>
      val values = value.asInstanceOf[java.util.ArrayList[String]]
      val scalaValues = values.asScala
      new ColTitles(key, scalaValues.toList)
    case "row" => new CellRow(key, value.asInstanceOf[Int])
    case "col" => new CellCol(key, value.asInstanceOf[Int])
  }
}

