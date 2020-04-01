package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.{Route, PathMatcher1}
import com.merit.modules.inventoryCount.InventoryCountBatchID
import com.merit.modules.inventoryCount.InventoryCountProductStatus

object GetInventoryCountBatchProducts extends Directives with JsonSupport {
  def apply(inventoryCountService: InventoryCountService): Route = {
    val batchIdMatcher: PathMatcher1[InventoryCountBatchID] =
      LongNumber.map(InventoryCountBatchID(_))

    (path(batchIdMatcher / "products") &
      parameters('page ? 1, 'rowsPerPage ? 10, 'status ? "all")) {
      (batchId, page, rowsPerPage, status) =>
        complete(
          inventoryCountService.getBatchProducts(
            batchId,
            InventoryCountProductStatus.fromString(status),
            page,
            rowsPerPage
          )
        )
    }
  }
}
