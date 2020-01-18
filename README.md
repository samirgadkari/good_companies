# good_companies
Find good companies based on their 10K statements.

This is a project that allows me to explore different libraries, and includes:
  - Jsoup to scrape www.sec.gov/edgar website and download Excel Files
  - Apache POI to read and pick out relevant data from Excel Spreadsheets
  - scala.collection.JavaConverters._ to convert between Java and Scala (since Java libraries are being used)
  - scala.collection.mutable to build mutable maps
  - SOLR search server to put documents in SOLR and query them using:
    - com.unique.companies.solr
    - org.apache.solr.client.solrj.impl._
    - org.apache.solr.common
    
This is how code is split among the files:
  - Some work on scala.concurrent.{ Future, blocking, Await } in asynchronous.scala
  - Load configuration from file into Config object in config.scala
  - Created a connection class to handle retries and timeout in conn.scala
  - In docInfo.scala:
    - java.time.{ LocalDate, YearMonth } to get date information for the company's 10K filings
    - scala.util.matching.Regex to get the dates, company name, and document type from strings
    - In edgarCompanies.scala does the orchestration to get each filing and extracts the balance sheet
  - extractXSSF.scala extracts relevant info from excel spreadsheet
  - item.scala builds information for each item in the spreadsheet
  - package.scala contains package-level Docs object
  - strOpers.scala is an object that handles string operations. ex. string to fiscal year end, string to date
  - solr/cloudClient.scala contains
    - CloudClient object to connect to SOLR server and close connection when done
    - CloudClient class creates maps of the spreadsheet with children under it. Also queries the collection in the SOLR server
    
