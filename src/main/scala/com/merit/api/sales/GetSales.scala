package com.merit.api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import com.merit.modules.sales.{SaleService, SaleFilters}
import org.joda.time.format.ISODateTimeFormat

object GetSales extends Directives with JsonSupport {
  val parser = ISODateTimeFormat.dateTimeParser()
  def apply(saleService: SaleService): Route =
    (get & pathEndOrSingleSlash & parameters(
      'page ? 1,
      'rowsPerPage ? 10,
      'startDate.?,
      'endDate.?
    )) { (page, rowsPerPage, startDate, endDate) =>
      complete(
        saleService
          .getSales(
            page,
            rowsPerPage,
            SaleFilters(
              startDate.map(parser.parseDateTime(_)),
              endDate.map(parser.parseDateTime(_))
            )
          )
      )
    }
}
