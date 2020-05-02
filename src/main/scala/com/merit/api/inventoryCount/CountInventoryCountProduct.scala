package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.Route
import api.CountInventoryCountProductRequest

object CountInventoryCountProduct extends Directives with JsonSupport {
  def apply(inventoryCountService: InventoryCountService): Route =
    (path("count-product") & entity(as[CountInventoryCountProductRequest])) { req =>
      complete(inventoryCountService.countProduct(req.id, req.count))
    }
}
