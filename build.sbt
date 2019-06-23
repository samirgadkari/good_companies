name := "companies"
version := "0.1.0"
scalaVersion := "2.12.3"
scalaVersion in ThisBuild := "2.12.3"
val playVersion = "2.6.7"
val solrjVersion = "7.3.0"
val jSoupVersion = "1.11.2"
val poiVersion   = "3.17"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.4" % "test"
libraryDependencies += "junit" % "junit" % "4.10" % Test

libraryDependencies ++= Seq(
  "org.jsoup" %  "jsoup" % jSoupVersion
)

libraryDependencies ++= Seq(
  "org.apache.poi" % "poi" % poiVersion,
  "org.apache.poi" % "poi-ooxml" % poiVersion,
  "org.apache.poi" % "poi-ooxml-schemas" % poiVersion
)

// val sparkVersion = "2.1.0"
// libraryDependencies ++= Seq(
//   "org.apache.spark" %% "spark-core" % sparkVersion,
//   "org.apache.spark" %% "spark-sql" % sparkVersion
// )

libraryDependencies += "com.typesafe.play" %% "play-json" % playVersion

libraryDependencies += "org.apache.solr" % "solr-solrj" % solrjVersion

