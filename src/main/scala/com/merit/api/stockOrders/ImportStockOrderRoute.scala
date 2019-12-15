package com.merit.api.stockOrders

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.stockOrders.StockOrderService
import com.merit.modules.products.ProductService
import com.merit.modules.excel.ExcelService
import akka.http.scaladsl.server.Route
import scala.concurrent.ExecutionContext
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import akka.http.scaladsl.model.StatusCodes.BadRequest

object ImportStockOrderRoute extends Directives with JsonSupport {
  def apply(
    stockOrderService: StockOrderService,
    excelService: ExcelService
  )(implicit ec: ExecutionContext): Route =
    (path("import") & formFields('date.as[String]) & uploadedFile("file")) {
      case (date, (_, file)) => {
        val rows = excelService.parseStockOrderImportFile(file)

        val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

        rows match {
          case Left(errors) => complete(BadRequest -> errors)
          case Right(rows) =>
            complete(
              stockOrderService.insertFromExcel(DateTime.parse(date, dateFormatter), rows)
            )
        }
      }
    }
}
