package com.merit.api.stockOrders

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.stockOrders.StockOrderService
import com.merit.modules.products.ProductService
import com.merit.modules.excel.ExcelService
import akka.http.scaladsl.server.Route
import scala.concurrent.ExecutionContext

object ImportStockOrderRoute extends Directives with JsonSupport {
  def apply(
    stockOrderService: StockOrderService,
    productService: ProductService,
    excelService: ExcelService
  )(implicit ec: ExecutionContext): Route =
    (path("import") & formFields('date.as[String]) & uploadedFile("file")) {
      case (date, (_, file)) => {
        val rows = excelService.parseStockOrderImportFile(file)

        for {
          existingProducts <- rows.map(r => productService.get(r.barcode))
          //! update the existing products
          
          //! create the non existing products
        } yield ()
        complete("ok")
      }
    }
}