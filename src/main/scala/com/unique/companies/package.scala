package com.unique.companies

package object Docs {

  type ProcessedDoc = List[Option[ProcessedSheet]]
  type ProcessedDocs = List[Option[ProcessedDoc]]

  object FiscalYrEnd {
    def apply(m: String, d: String) =
      new FiscalYrEnd(m.toInt, d.toInt)
  }
  class FiscalYrEnd(val m: Int, val d: Int) {
    override def toString() = "%02d-%02d".format(m, d)
  }
}
