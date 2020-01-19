package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.Route

object GetInventoryCountBatches extends Directives with JsonSupport {
  def apply(inventoryCountService: InventoryCountService): Route = {
    (get & pathEndOrSingleSlash & parameter('page ? 1, 'rowsPerPage ? 10)) {
      (page, rowsPerPage) =>
        complete(inventoryCountService.getBatches(page, rowsPerPage))
    }
  }
}