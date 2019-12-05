package com.merit.api.products
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO
import java.nio.file.Paths
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes

object GetProductTemplateRoute extends Directives {
  def apply(): Route =
    (path("product-import-template") & get) {
      val source =
        FileIO.fromPath(
          Paths.get("src/main/resources/excelTemplates/product-import-template.xlsx")
        )

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
