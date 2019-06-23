package com.unique.companies.solr

import org.apache.solr.client.solrj.impl._
import org.apache.solr.common.params.MapSolrParams
import org.apache.solr.common.{ SolrDocumentList, SolrInputField, SolrInputDocument }
import scala.collection.mutable
import scala.collection.JavaConverters._
import com.unique.companies.{ Item, ProcessedSheet }
import com.unique.companies.Docs._

object CloudClient {

  val cache = mutable.Map[String, (CloudClient, CloudSolrClient)]()

  def apply(url: String): CloudClient = {

    cache.get(url) match {
      case Some((cloudC, cloudSolrC)) => cloudC
      case None =>
        val cloudSolrC = new CloudSolrClient.Builder(List[String](url).asJava)
          .build()

        val client = new CloudClient(url, cloudSolrC)
        cache += url -> (client, cloudSolrC)
        client
    }
  }

  def close(url: String) = {
    cache.get(url) match {
      case Some((cloudC, cloudSolrC)) =>
        cloudSolrC.close()
        cache -= url
      case None =>
        throw new IllegalArgumentException("invalid url")
    }
  }
}

class CloudClient(url: String, cloudSolrC: CloudSolrClient) {

  def addFieldToMap(
    m:    mutable.Map[String, SolrInputField],
    name: String,
    v:    Any
  ) = {

    val inputField = new SolrInputField(name)
    inputField.addValue(v)
    m += name -> inputField
  }

  def makeParent(sheet: ProcessedSheet): SolrInputDocument = {

    var fields = mutable.Map[String, SolrInputField]()
    addFieldToMap(fields, "id", sheet.id)
    addFieldToMap(fields, "cik", sheet.cik)
    addFieldToMap(fields, "accessor_num", sheet.accNum)
    addFieldToMap(fields, "sheet_name", sheet.sheetName)
    sheet.docType match {
      case Some(x) => addFieldToMap(fields, "document_type", x)
      case None    =>
    }
    sheet.docPeriodEndDate match {
      case Some(x) =>
        import java.time.ZoneOffset
        import java.time.format.DateTimeFormatter

        // println(s"LocalDate: $x")
        val zonedDateTime = x.atStartOfDay(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ISO_INSTANT
        val outputDateTime = zonedDateTime.format(formatter)
        // println(s"docPeriodEndDate: $outputDateTime")
        addFieldToMap(fields, "document_period_end_date", outputDateTime)
      case None =>
    }
    sheet.entityRegName match {
      case Some(x) => addFieldToMap(fields, "entity_registrant_name", x)
      case None    =>
    }
    sheet.fiscalYrEndDate match {
      case Some(x) =>
        addFieldToMap(fields, "current_fiscal_year_end_date", x.toString)
      case None =>
    }
    addFieldToMap(fields, "content_type", sheet.contentType)

    val parent = new SolrInputDocument(fields.asJava)
    parent
  }

  def makeChild(item: Item): SolrInputDocument = {

    var fields = mutable.Map[String, SolrInputField]()
    addFieldToMap(fields, "id", item.id)
    addFieldToMap(fields, "row", item.row)
    addFieldToMap(fields, "col", item.col)
    addFieldToMap(fields, "cell_text", item.cellTitle)
    item.data match {
      case Some(x) =>
        addFieldToMap(fields, "cell_value", x)
      case None =>
    }
    item.rowTitles foreach { rt =>
      {
        if (rt != "") addFieldToMap(fields, "row_titles", rt)
      }
    }
    item.colTitles foreach { ct =>
      {
        if (ct != "") addFieldToMap(fields, "col_titles", ct)
      }
    }

    val child = new SolrInputDocument(fields.asJava)
    child
  }

  def index(sheet: ProcessedSheet): Unit = {

    val parentDoc = makeParent(sheet)
    val children = sheet.items map (item => {
      val childDoc = makeChild(item)
      parentDoc.addChildDocument(childDoc)
    })
    val updateResp = cloudSolrC.add("companies", parentDoc)
    cloudSolrC.commit("companies")
  }

  import com.unique.companies.Result

  def query(
    collection:  String,
    queryParams: Map[String, String]
  ): List[(String, List[Result])] = {

    val qParams = new MapSolrParams(queryParams.asJava)
    CloudClient.cache.get(url) match {
      case Some((solrC, httpSolrC)) =>
        val queryResponse = httpSolrC query (collection, qParams)

        val ds: SolrDocumentList = queryResponse.getResults()
        val docs = ds.asScala.toList

        for {
          doc <- docs
        } yield {

          val id = doc.get("id").asInstanceOf[String]
          // val children = doc.getChildDocuments().asScala.toList
          // println(s"number of children: ${children.length}")
          val results = {
            for (entry <- doc.entrySet.asScala) yield {
              val field = entry.getKey
              val value = entry.getValue
              Result(field, value)
            }
          }.toList
          (id, results)
        }
      case None =>
        throw new IllegalArgumentException("invalid query")
    }
  }
}

