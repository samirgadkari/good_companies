/*
package com.unique.companies

import org.apache.spark.sql._
import org.apache.spark.sql.types._

object DB {

  import org.apache.spark.sql.SparkSession
  import org.apache.spark.sql.functions._

  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Companies")
      .config("spark.master", "local")
      .getOrCreate()

  def createDB(db: String): this.type = {
    spark.sql("CREATE DATABASE " + db)
    this
  }

  /*  def useDB() : this.type = {
    spark.sql("USE DATABASE " + db)
    this
  }
*/

  def dbExists(db: String) = spark.catalog.databaseExists(db)

  def useDB(db: String): this.type = {
    spark.sql("USE " + db)
    this
  }

  def createTable(s: String): this.type = {
    spark.sql(s)
    spark.catalog.listDatabases.show(false)
    this
  }

  def tableExists(table: String) = spark.catalog.tableExists(table)

  def listColumns(db: String, table: String) =
    spark.catalog.listColumns(db, table)
}
 */
