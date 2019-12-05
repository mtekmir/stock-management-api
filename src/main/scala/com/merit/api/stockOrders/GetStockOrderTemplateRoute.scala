package com.merit.api.stockOrders
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO
import java.nio.file.Paths
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.MediaTypes

object GetStockOrderTemplateRoute extends Directives {
  def apply(): Route = {
    (path("stock-order-template") & get) {
      val source =
        FileIO.fromPath(Paths.get("src/main/resources/excelTemplates/stock-order-template.xlsx"))

      complete(
        HttpResponse(
          status = StatusCodes.OK,
          entity = HttpEntity(
            contentType =
              MediaTypes.`application/vnd.openxmlformats-officedocument.spreadsheetml.template`,
            source
          )
        )
      )
    }
  }
}