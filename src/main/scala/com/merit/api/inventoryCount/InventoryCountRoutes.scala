package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.Route
import com.merit.modules.excel.ExcelService
import scala.concurrent.ExecutionContext

object InventoryCountRoutes extends Directives {
  def apply(inventoryCountService: InventoryCountService, excelService: ExcelService)(
    implicit ec: ExecutionContext
  ): Route =
    pathPrefix("inventory-count") {
      CreateInventoryCountBatch(inventoryCountService) ~
      CountInventoryCountProduct(inventoryCountService) ~
      GetInventoryCountBatches(inventoryCountService) ~
      GetInventoryCountBatchProducts(inventoryCountService) ~
      GetInventoryCountBatch(inventoryCountService) ~
      SearchInventoryCountProducts(inventoryCountService)
    }
}
