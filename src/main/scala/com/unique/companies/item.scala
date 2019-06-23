package com.unique.companies

object Item {

  def apply(id: String, row: Int, col: Int,
            cellTitle: String, data: Option[Double],
            rowTitles: List[String],
            colTitles: List[String]) = {

    new Item(id, row, col, cellTitle, data,
      rowTitles filterNot StringUtil.isNum,
             colTitles)
  }
}

class Item(
  val id:        String,
  val row:       Int,
  val col:       Int,
  val cellTitle: String,
  val data:      Option[Double],
  val rowTitles: List[String],
  val colTitles: List[String]
) {
  override def toString() = {
    val d = data match {
      case Some(s) => s.toString
      case None    => ""
    }
    val rt = rowTitles.toString
    val ct = colTitles.toString
    "(%3d,%3d) TITLE: %s DATA: %s ROWTITLES: %s COLTITLES: %s\n".
      format(row, col, cellTitle, d, rt, ct)
  }
}

