package com.merit.api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.sales.SaleService
import com.merit.modules.excel.ExcelService
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import org.joda.time.DateTime
import com.merit.modules.products.Currency
import org.joda.time.format.DateTimeFormat

object ImportPastSaleRoute extends Directives with JsonSupport {
  def apply(saleService: SaleService, excelService: ExcelService): Route =
    (path("import-past-sale") & formFields('date.as[String], 'total.as[String]) & uploadedFile(
      "file"
    )) {
      case (date, total, (metadata, file)) =>
        val rows          = excelService.parseSaleImportFile(file)
        val dateFormatter = DateTimeFormat.forPattern("dd.MM.yyyy")

        rows match {
          case Left(error) => complete(StatusCodes.BadRequest -> error)
          case Right(rows) =>
            complete(
              saleService.importPastStoreSales(
                rows,
                DateTime.parse(date, dateFormatter),
                Currency.fromOrZero(total)
              )
            )
        }

    }
}
