package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.PathMatcher1
import com.merit.modules.inventoryCount.InventoryCountProductID

object DeleteInventoryCountProduct extends Directives with JsonSupport {
  def apply(inventoryCountService: InventoryCountService): Route = {
    val productId: PathMatcher1[InventoryCountProductID] =
      LongNumber.map(InventoryCountProductID(_))

    (delete & path("product" / productId)) { id =>
      complete(inventoryCountService.deleteInventoryCountProduct(id))
    }
  }
}
