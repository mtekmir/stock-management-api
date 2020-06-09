package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.PathMatcher1
import com.merit.modules.inventoryCount.InventoryCountBatchID

object DeleteInventoryCount extends Directives with JsonSupport {
  def apply(inventoryCountService: InventoryCountService): Route = {
    val batchIdMatcher: PathMatcher1[InventoryCountBatchID] =
      LongNumber.map(InventoryCountBatchID(_))

    (delete & path(batchIdMatcher) & pathEnd) { (batchId) =>
      complete(inventoryCountService.delete(batchId))
    }
  }
}
