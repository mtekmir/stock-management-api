package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.Route
import api.CreateInventoryCountRequest
import akka.http.scaladsl.model.StatusCodes

object CreateInventoryCountBatch extends Directives with JsonSupport {
  def apply(inventoryCountService: InventoryCountService): Route =
    (pathEndOrSingleSlash & entity(as[CreateInventoryCountRequest])) { req =>
      (req.brandId, req.categoryId) match {
        case (None, None) =>
          complete(StatusCodes.BadRequest -> "Please choose either a category or a brand")
        case _ =>
          complete(inventoryCountService.create(req.name, req.brandId, req.categoryId))
      }
    }
}
