package com.unique.companies

import java.io._
import org.jsoup._
import org.jsoup.nodes.Element
import scala.collection.JavaConverters._
import java.util.regex._
import scala.concurrent.duration._

object Companies extends App {

  def buildCompaniesList(): Unit = {
    val allCompanies = AllCompanies.getAllCompaniesList

    val bw = new BufferedWriter(new FileWriter("allCompanies.txt"))

    try {
      allCompanies foreach { entry =>
        {
          bw.write(entry._1 + " ---> " + entry._2._1 + " ---> " +
            entry._2._2 + " ---> " + entry._2._3 + " ---> [")
          entry._2._4 match {
            case None => bw.write("None")
            case Some(v) => v foreach { e =>
              bw.write(e + ",")
            }
          }
          bw.write("]\n")
        }
      }
    } finally {
      bw.close
    }
  }

  val conf = Config.load()

  AllCompanies.tenKs(conf, "allCompanies_1.txt")

  AllCompanies.index10Kdata(conf)
  AllCompanies.balanceSheets(conf)

  /*  if (!DB.dbExists("test")) DB.createDB("test").useDB("test")
  if (!DB.tableExists("pet"))
    DB.createTable(
      s"""CREATE TABLE pet (name VARCHAR(20), owner VARCHAR(20),
          species VARCHAR(20), sex CHAR(1), birth DATE, death DATE)""")

  println(s"Database test, Table pet = ${DB.listColumns("test", "pet")}")
*/
}

