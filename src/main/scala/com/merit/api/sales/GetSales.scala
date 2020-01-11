package com.merit.api.sales
import akka.http.scaladsl.server.Directives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import com.merit.modules.sales.SaleService

object GetSales extends Directives with JsonSupport {
  def apply(saleService: SaleService): Route =
    (get & pathEndOrSingleSlash & parameter('page ? 1, 'rowsPerPage ? 10)) {
      (page, rowsPerPage) =>
        complete(saleService.getSales(page, rowsPerPage))
    }
}
