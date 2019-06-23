package com.unique.companies

import org.apache.poi.ss.usermodel.{ WorkbookFactory, DataFormatter, Cell, Row, Sheet }
import org.apache.poi.ss.util.CellRangeAddress
import java.io.{ File, FileOutputStream }
import scala.collection.JavaConverters._
import scala.collection.mutable
import java.time.{ LocalDate, YearMonth }
import Docs._

object ProcessedSheet {
  def apply(id: String, cik: String, accNum: String,
            sheetName: String, docType: Option[String],
            docPeriodEndDate: Option[LocalDate], entityRegName: Option[String],
            fiscalYrEndDate: Option[Docs.FiscalYrEnd], contentType: String,
            items: List[Item]) =
    new ProcessedSheet(id, cik, accNum, sheetName, docType,
                       docPeriodEndDate, entityRegName,
                       fiscalYrEndDate, contentType, items)
}

class ProcessedSheet(val id: String, val cik: String, val accNum: String,
                     val sheetName: String, val docType: Option[String],
                     val docPeriodEndDate: Option[LocalDate], val entityRegName: Option[String],
                     val fiscalYrEndDate: Option[Docs.FiscalYrEnd], val contentType: String,
                     val items: List[Item])

object ExtractXSSF {

  type CellAddr = (Int, Int)
  type SheetName = String

  class MergedRegion(
    val firstRow: Int, val firstCol: Int,
    val lastRow: Int, val lastCol: Int
  ) {

    def apply(): (CellAddr, CellAddr) =
      ((firstRow, firstCol), (lastRow, lastCol))

    override def toString() =
      "((%3s, %3s), (%3s, %3s))".format(firstRow, firstCol, lastRow, lastCol)
  }

  class CellInfo(val addr: CellAddr, val data: String) {
    override def toString() =
      "addr: (%3s, %3s), data: %s".format(addr._1, addr._2, data.toString)
  }

  case class SheetInfo(
    val name:          String,
    val mergedRegions: List[MergedRegion],
    val cells:         List[CellInfo]
  ) {

    override def toString() = {

      "sheet name: " + name + "\n" +
        "mergedRegions: \n" + "\t" + mergedRegions.mkString("\n\t") + "\n" +
        "cells: \n" + "\t" + cells.mkString("\n\t")
    }
  }

  class WB(val sheets: List[SheetInfo]) {

    def addSheet(s: SheetInfo): WB = new WB(s :: sheets)

    override def toString() = (sheets map (_.toString)).mkString("\n\n")
  }

  def buildData(file: File) = {

    val workbook = WorkbookFactory.create(file)

    def buildCells(cells: List[Cell]): List[CellInfo] = {

      cells map { c: Cell =>
        new CellInfo((c.getAddress.getRow, c.getAddress.getColumn), c.toString)
      }
    }

    def buildSheet(sheet: Sheet): SheetInfo = {

      val allCells: List[CellInfo] =
        sheet.rowIterator.asScala.toList flatMap {
          row: Row => buildCells(row.cellIterator.asScala.toList)
        }

      val mergedRs = sheet.getMergedRegions.asScala.toList map { mr =>

        new MergedRegion(mr.getFirstRow, mr.getFirstColumn,
                         mr.getLastRow, mr.getLastColumn)
      }

      new SheetInfo(sheet.getSheetName, mergedRs, allCells)
    }

    val res = new WB(workbook.sheetIterator.asScala.toList map {
      s: org.apache.poi.ss.usermodel.Sheet => buildSheet(s)
    })

    res
  }

  def xssfToProcessedDoc(wb: WB, cik: String, accNum: String): Option[ProcessedDoc] = {

    val maxNumTitles = 3

    def buildMap(mrs: List[MergedRegion], cells: List[CellInfo]): mutable.Map[(Int, Int), String] = {

      var m = mutable.Map[(Int, Int), String]()
      cells foreach (cell => {
        m += (cell.addr._1, cell.addr._2) -> cell.data
      })

      mrs foreach (mr => {
        val mainRow = mr.firstRow
        val mainCol = mr.firstCol

        m exists (_._1 == (mainRow, mainCol)) match {
          case true =>
            for (row <- mr.firstRow to mr.lastRow) {
              for (col <- mr.firstCol to mr.lastCol) {
                m += (row, col) -> m((mainRow, mainCol))
              }
            }
          case false =>
        }
      })

      m
    }

    def gatherNums(
      id:    (String, Int),
      mrs:   List[MergedRegion],
      cells: List[CellInfo],
      m:     mutable.Map[(Int, Int), String]
    ): (List[Item], Int) = {

      var idx = id._2
      val cellsWithNum = cells filter (cell => StringUtil.isNum(cell.data))

      val itemList = for (cell <- cellsWithNum) yield {

        val (row, col, str) = (cell.addr._1, cell.addr._2, cell.data)

        val (rt, ct) = (
          (for (idx <- 0 until maxNumTitles) yield {
            m exists (_._1 == ((row, idx))) match {
              case true =>
                if ((idx == col) || StringUtil.isNum(m((row, idx)))) ""
                else {
                  m((row, idx))
                }
              case false => ""
            }
          }).toList,
          (for (idx <- 0 until maxNumTitles) yield {
            m exists (_._1 == ((idx, col))) match {
              case true =>
                if ((idx == row) || StringUtil.isNum(m((idx, col)))) ""
                else {
                  m((idx, col))
                }
              case false => ""
            }
          }).toList
        )

        idx = idx + 1
        Item(id._1 + "_" + idx, row, col, cell.data, StringUtil.num(str), rt, ct)
      }

      (itemList, idx)
    }

    def processDocEntityInfoSheet(
      id:       (String, Int),
      mrs:      List[MergedRegion],
      cells:    List[CellInfo],
      startRow: Int,
      startCol: Int,
      m:        mutable.Map[(Int, Int), String]
    ): (List[Item], Int) = {

      var idx = id._2
      val cellsInRange = cells filter (cell =>
        cell.addr._1 >= startRow &&
          cell.addr._2 >= startCol)

      val itemList = for (cell <- cellsInRange) yield {

        val (row, col, str) = (cell.addr._1, cell.addr._2, cell.data)

        val (rt, ct) = (
          (for (idx <- 0 until maxNumTitles) yield {
            m exists (_._1 == (row, idx)) match {
              case true  => m((row, idx))
              case false => ""
            }
          }).toList,
          (for (idx <- 0 until maxNumTitles) yield {
            m exists (_._1 == (idx, col)) match {
              case true  => m((idx, col))
              case false => ""
            }
          }).toList
        )

        idx = idx + 1
        Item(id._1 + "_" + idx, row, col, cell.data, StringUtil.num(str), rt, ct)
      }

      (itemList, idx)
    }

    def docEntityInfoSheet(sheets: List[SheetInfo]): Option[SheetInfo] = {

      assert(sheets.length > 0)
      val s =
        sheets.filter(sheet => {
          sheet.name.replace('_', ' ')
            .toLowerCase
            .startsWith("document and entity")
        })

      if (s.length > 0) Some(s.head)
      else {
        val s = sheets.filter(sheet => {
          sheet.name.replace('_', ' ')
            .toLowerCase
            .startsWith("dei ")
        })
        if (s.length > 0) Some(s.head)
        else None
      }
    }

    import play.api.libs.json._

    implicit val itemWrites = new Writes[Item] {
      def writes(e: Item) = Json.obj(
        "id" -> e.id,
        "row" -> e.row,
        "col" -> e.col,
        "cellText" -> e.cellTitle,
        "cellValue" -> e.data,
        "rowTitles" -> Json.toJson(e.rowTitles),
        "colTitles" -> Json.toJson(e.colTitles)
      )
    }

    assert(wb.sheets.length > 0)
    val eiSheet = docEntityInfoSheet(wb.sheets)
    eiSheet match {
      case Some(entityInfoSheet) =>

        val m = buildMap(
          entityInfoSheet.mergedRegions,
          entityInfoSheet.cells
        )

        val (items, lastIdx) = processDocEntityInfoSheet(
          (cik + "_" + accNum + "_" + entityInfoSheet.name, -1),
          entityInfoSheet.mergedRegions,
          entityInfoSheet.cells,
          2, // start row
          1, // start col
          m
        )

        val docInfo = DocInfo(items)
        // println(s"$docInfo")

        import com.unique.companies.solr._

        var idx = lastIdx
        Some(for (sheet <- wb.sheets) yield {

          val m = buildMap(
            sheet.mergedRegions,
            sheet.cells
          )

          val id = cik + "_" + accNum + "_" + sheet.name + "_" + idx

          val (items2, lastIdx2) =
            gatherNums((id, idx), sheet.mergedRegions, sheet.cells, m)

          idx = lastIdx2

          // Remove spaces on either end of sheet name.
          // Replace sheet name '_' with ' '.
          // Make name lowercase
          val sheetName = sheet.name.trim.replace('_', ' ').toLowerCase

          Some(ProcessedSheet(id, cik, accNum, sheetName,
                              docInfo.typ,
                              docInfo.periodEndDate,
                              docInfo.entityRegName,
                              docInfo.fiscalYrEndDate,
            "parentDocument", items2))
        })
      case None => None
    }
  }
}
