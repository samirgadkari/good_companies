package com.unique.companies

import org.jsoup._
import org.jsoup.nodes.{ Element, Document }
import scala.collection.JavaConverters._
import java.util.regex._
import scala.util.Try
import java.io._
import Docs._

object AllCompanies {

  type CIK = String
  type CompanyName = String
  type SIC = String // SIC = standard industrial classification
  // (ex. 6189)
  type SICText = String // SICText = name associated with this SIC classification
  // (ex. "ASSET-BACKED SECURITIES")
  type AccNum = String // AccNum = Accessor Number

  val searchURLbase = "https://www.sec.gov"

  def companiesListPage(company: String, start: Int, count: Int): Map[CIK, (CompanyName, Option[SIC], Option[SICText])] = {
    val params: Map[String, String] = Map[String, String](
      "action" -> "getcompany",
      "company" -> company,
      "owner" -> "exclude",
      "match" -> "",
      "start" -> "%d".format(start),
      "hidefilings" -> "0"
    )

    val searchResponse: Option[Connection.Response] =
      Conn.connect(
        searchURLbase + "/cgi-bin/browse-edgar",
        Connection.Method.GET, params, true
      )

    searchResponse match {
      case None => Map[CIK, (CompanyName, Option[SIC], Option[SICText])]()
      case Some(res) => {
        try {
          val searchRespDoc = res.parse();
          val cik: Array[Element] = searchRespDoc
            .select("div#seriesDiv > table > tbody > tr:gt(0) > td:first-child").asScala.toArray
          val nameAndSICcode: Array[Element] = searchRespDoc
            .select("div#seriesDiv > table > tbody > tr:gt(0) > td:nth-child(3n+2)")
            .asScala.toArray

          val cikText = cik map { e => e.text }
          val nameAndSICcodeText = nameAndSICcode map { e =>
            val doc: Document = org.jsoup.Jsoup.parse(e.html)
            doc.body.text
          }

          val nameAndSICcodeRegex = Pattern.compile("""^(.*)\s*SIC:\s*(\d*)\s*-\s*(.*)$""")
          val all: Map[CIK, (CompanyName, Option[SIC], Option[SICText])] =
            (cikText zip (nameAndSICcodeText map { text =>
              val m: Matcher = nameAndSICcodeRegex.matcher(text)
              if (m.find()) {
                val name = m.group(1)
                val SIC = m.group(2)
                val SICtext = m.group(3)

                (name.trim, Some(SIC), Some(SICtext))
              } else {

                (text.trim, None, None)
              }
            })).toMap

          all
        } catch {
          case ex: Exception => {
            println(s"Exception occurred: $ex")
            Map[CIK, (CompanyName, Option[SIC], Option[SICText])]()
          }
        }
      }
    }
  }

  def getAllCompaniesList(): Map[CIK, (CompanyName, Option[SIC], Option[SICText], Option[Array[AccNum]])] = {

    val maxCompaniesPerPage = 100
    val maxCompaniesPerChar = 100000

    val ranges =
      for {
        c <- 'a' to 'z'
        idx <- 0 to maxCompaniesPerChar by maxCompaniesPerPage
      } yield {
        (c, idx)
      }

    val res: Map[CIK, (CompanyName, Option[SIC], Option[SICText], Option[Array[AccNum]])] =
      (ranges flatMap ({
        case (name, start) =>
          println(s"name: $name, start: $start")
          val onePageRes =
            companiesListPage(name.toString, start, maxCompaniesPerPage)

          if (onePageRes.size > 0) {
            onePageRes map (entry => {
              val accessNumbers = accNum(entry._1)

              (entry._1, (entry._2._1, entry._2._2,
                entry._2._3, accessNumbers))
            })
          } else {
            println(s"start: $start")
            Map[CIK, (CompanyName, Option[SIC], Option[SICText], Option[Array[AccNum]])]()
          }
      })).toMap

    res
  }

  def accNum(cik: CIK): Option[Array[AccNum]] = {

    val params: Map[String, String] = Map[String, String](
      "action" -> "getcompany",
      "CIK" -> cik,
      "owner" -> "exclude",
      "type" -> "10-K"
    )

    val searchResponse: Option[Connection.Response] =
      Conn.connect(
        searchURLbase + "/cgi-bin/browse-edgar",
        Connection.Method.GET,
        params,
        true
      )

    searchResponse match {
      case None => None
      case Some(res) => {
        try {
          val searchRespDoc = res.parse();
          val tableData: Array[Element] = searchRespDoc.
            select("div#seriesDiv > table > tbody > tr:gt(0) > td:nth-child(5n+2)").
            asScala.toArray

          val accessNumbers = tableData map (text => {
            val regexAccNum = Pattern.compile("""accession_number=([\d-]*)&""", Pattern.DOTALL)
            val matcher = regexAccNum.matcher(text.html)
            if (matcher.find()) {
              val accessionNum = matcher.group(1);
              accessionNum
            } else {
              ""
            }
          }) filterNot (v => v.length == 0)

          if (accessNumbers.size > 0) Some(accessNumbers)
          else None
        } catch {
          case ex: Exception =>
            println(s"Exception occurred: $ex")
            None
        }
      }
    }
  }

  def getCompanies10Kdata(cik: CIK, acc: AccNum): Array[Byte] = {

    val cikText = cik.dropWhile(_ == '0')
    println(s"cikText: $cikText")

    val params: Map[String, String] = Map[String, String](
      "type" -> "10-K",
      "action" -> "getcompany",
      "CIK" -> cik
    )

    Conn.connect(
      "https://www.sec.gov/cgi-bin/browse-edgar",
      Connection.Method.GET,
      params,
      true
    ) match {
        case None => Array[Byte]()
        case Some(res: Connection.Response) =>

          async.wait(async.secondsToDuration(2))

          Conn.connect(
            "https://www.sec.gov/Archives/edgar/data/" + cikText + "/" +
              acc.filterNot("-".toSet) +
              "/" + "Financial_Report.xlsx",
            Connection.Method.GET,
            params,
            true
          ) match {
              case None                           => Array[Byte]()
              case Some(res: Connection.Response) => res.bodyAsBytes
            }
      }
  }

  def get10Kdata(conf: Config.Cfg, cik: String, acc: String): Unit = {
    def saveData(name: String, b: Array[Byte]): Unit = {
      if (b.size == 0) println(s"File: ${name} empty")
      else {
        val out = new FileOutputStream(name);
        out.write(b)
        out.close
      }
    }

    val dst = conf("xlsx")
    val filename = dst + "/" + cik + "_" + acc + ".xlsx"

    val file = new File(filename)
    if (file.exists())
      println("  ... file exists")
    else {
      val data = AllCompanies.getCompanies10Kdata(cik, acc)
      saveData(filename, data)
    }
  }

  def tenKs(conf: Config.Cfg, filename: String) = {

    import scala.io.Source

    val src = conf("base") + "/" + filename
    for (line <- Source.fromFile(src).getLines()) {

      val wholeLine =
        """^(.*?)\s--->\s(.*?)\s--->\s(Some<.*?>|None)\s--->\s(Some<.*?>|None)\s--->\s\[(.*?)\]$""".r
      line match {
        case wholeLine(cik, name, sic, cikText, accNums) =>

          if (accNums != "None") {
            val accessNumbers = accNums dropRight 1 split ","
            accessNumbers foreach { acc =>

              println(s"Getting 10K for: cik: $cik, acc: $acc")

              try {
                get10Kdata(conf, cik, acc)
              } catch {
                case ex: Throwable => println(s"Caught exception: $ex ... continuing ...")
              }
            }
          }
        case _ => println(s"unmatched line: $line")
      }
    }
  }

  import com.unique.companies.solr._

  def index10Kdata(conf: Config.Cfg): Unit = {

    import java.io.PrintWriter

    val srcDir = conf("xlsx")
    val dstDir = conf("json")

    val srcDirFile = new File(srcDir)
    val solrUrl = "http://localhost:8983/solr"
    val client = CloudClient(solrUrl)

    for {
      name <- srcDirFile.listFiles.toList.map(_.getName).map(_.dropRight(5))
      if (name.length > 11) // To ignore .* files
    } yield {

      val (cik, accNum) = StringUtil.stringToTuple(name, '_')
      val srcFilename = srcDir + "/" + name + ".xlsx"
      print(s"Extracting: $srcFilename ... ")
      val srcFile = new File(srcFilename)
      val wb = ExtractXSSF.buildData(srcFile)
      val processedDoc = ExtractXSSF.xssfToProcessedDoc(wb, cik, accNum)
      print("indexing ... ")
      indexDoc(conf, processedDoc, client)
      println("done")
    }

    CloudClient.close(solrUrl)
  }

  def indexDoc(
    conf:   Config.Cfg,
    optDoc: Option[ProcessedDoc],
    client: CloudClient
  ): Unit = {

    optDoc match {
      case Some(processedDoc) =>
        processedDoc foreach ((optSheet: Option[ProcessedSheet]) => {
          optSheet match {
            case Some(sheet) => client.index(sheet)
            case None        =>
          }
        })
      case None =>
    }
  }

  def balanceSheets(
    conf: Config.Cfg
  // procSheets: List[Option[List[ProcessedSheet]]]
  ): Unit = {

    val queryParams = Map[String, String](
      "q" -> """/(consolidated|condensed|combined)?\ ?balance sheet(s)?/""",
      "fl" -> "*",
      "rows" -> Int.MaxValue.toString,
      "start" -> "0"
    )

    val solrUrl = "http://localhost:8983/solr"
    val solrClient = CloudClient(solrUrl)
    val queryResponse = solrClient query ("companies", queryParams)

    import scala.collection.mutable
    var i = 0
    var m = mutable.Map[String, String]()
    val re = """^\s*[0-9\-]*\s*$"""
    queryResponse foreach { doc: (String, List[Result]) =>
      {
        print(s"Document ID: ${doc._1}, ")
        val listOfStr = doc._1.split('_').toList
        val parts = listOfStr filterNot (str => str matches re)
        m += parts.mkString -> doc._1

        doc._2 foreach { res =>

          res match {
            case r: Version              => print(s"version: $r, ")
            case r: ID                   => print(s"id: $r, ")
            case r: CentralIndexKey      => print(s"cik: $r, ")
            case r: AccessorNumber       => print(s"accessor_num: $r, ")
            case r: SheetName            => print(s"sheet_name: $r, ")
            case r: DocumentType         => print(s"document_type: $r, ")
            case r: DocPeriodEndDate     => print(s"document_period_end_date: $r, ")
            case r: EntityRegistrantName => print(s"entity_registrant_name: $r, ")
            case r: FiscalYearEndDate    => print(s"current_fiscal_year_end_date: $r, ")
            case r: ContentType          => print(s"content_type: $r, ")

            case r: CellText             => print(s"cell_text: $r, ")
            case r: CellValue            => print(s"cell_value: $r, ")
            case r: RowTitles            => print(s"row_titles: $r, ")
            case r: ColTitles            => print(s"col_titles: $r, ")
            case r: CellRow              => print(s"row: $r, ")
            case r: CellCol              => print(s"col: $r, ")
          }
        }
        println()
      }
    }

    println(s"num docs: ${queryResponse.length}")
  }

  def incomeStatement(conf: Config.Cfg): Unit = {
  }
}

