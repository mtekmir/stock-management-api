package com.merit.api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.excel.ExcelService
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes.{BadRequest}
import org.joda.time.format.DateTimeFormat
import com.merit.modules.sales.SaleService
import org.joda.time.DateTime
import com.merit.modules.products.Currency
import com.merit.modules.users.UserID

object ImportWebSalesRoute extends Directives with JsonSupport {
  def apply(saleService: SaleService, excelService: ExcelService, 
  userId: UserID
  ): Route =
    (path("import") & uploadedFile("file")) {
      case (metadata, file) =>
        val rows = excelService.parseSaleImportFile(file)

        rows match {
          case Left(error) => complete(BadRequest -> error)
          case Right(rows) =>
            complete(saleService.importSoldProductsFromWeb(rows, userId))
        }

    }
}
