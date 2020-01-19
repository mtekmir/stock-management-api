package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.Route
import api.CreateInventoryCountRequest

object CreateInventoryCountBatch extends Directives with JsonSupport {
  def apply(inventoryCountService: InventoryCountService): Route =
    (pathEndOrSingleSlash & entity(as[CreateInventoryCountRequest])) { req =>
      complete(inventoryCountService.create(req.name, req.brandId, req.categoryId))
    }
}
