package com.merit.api.sales

import akka.http.scaladsl.server.Directives
import com.merit.api.AuthDirectives
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO
import java.nio.file.Paths
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ContentTypes

object GetSaleTemplateRoute extends Directives {
  def apply(): Route =
    (path("sale-excel-template") & get) {
      val source =
        FileIO.fromPath(Paths.get("src/main/resources/excelTemplates/sale-template.xlsx"))

      complete(
        HttpResponse(
          status = StatusCodes.OK,
          entity = HttpEntity(
            contentType =
              MediaTypes.`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`,
            source
          )
        )
      )
    }
}
