package com.merit.api.stockOrders

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.stockOrders.StockOrderService
import com.merit.modules.products.ProductService
import akka.http.scaladsl.server.Route
import com.merit.modules.excel.ExcelService
import scala.concurrent.ExecutionContext

object StockOrderRoutes extends Directives with JsonSupport {
  def apply(
    stockOrderService: StockOrderService,
    excelService: ExcelService
  )(implicit ec: ExecutionContext): Route =
    pathPrefix("stock-orders") {
      ImportStockOrderRoute(stockOrderService, excelService) ~
      GetStockOrderTemplateRoute()
    }
}
