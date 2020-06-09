package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import scala.concurrent.ExecutionContext
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import akka.http.scaladsl.model.StatusCodes.BadRequest
import com.merit.modules.inventoryCount.InventoryCountService
import com.merit.modules.excel.ExcelService

object ImportInventoryCount extends Directives with JsonSupport {
  def apply(
    inventoryCountService: InventoryCountService,
    excelService: ExcelService
  )(implicit ec: ExecutionContext): Route =
    (path("import") & formFields('date.as[String]) & uploadedFile("file")) {
      case (date, (_, file)) => {
        val rows = excelService.parseInventoryCountImportFile(file)

        val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

        rows match {
          case Left(errors) => complete(BadRequest -> errors)
          case Right(rows) =>
            complete(
              inventoryCountService.insertFromExcel(DateTime.parse(date, dateFormatter), rows)
            )
        }
      }
    }
}
