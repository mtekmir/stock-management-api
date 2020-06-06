package com.merit.modules

import slick.dbio.DBIO
import com.merit.modules.products.ProductRow
import scala.concurrent.ExecutionContext

package object inventoryCount {

  import com.merit.modules.products.ProductDTO
  implicit class C2(
    val p: ProductDTO
  ) {
    def toInventoryCountProductRow(
      batchId: InventoryCountBatchID
    ): InventoryCountProductRow = {
      import p._
      InventoryCountProductRow(
        batchId,
        id,
        expected = qty
      )
    }
  }

  implicit class C1(
    val queryResult: DBIO[Seq[(InventoryCountProductRow, ProductRow)]]
  )(implicit ec: ExecutionContext) {
    def toInventoryCountProductDTOS =
      queryResult.map {
        _.map {
          case (batchProductRow, productRow) =>
            InventoryCountProductDTO.fromRow(batchProductRow, productRow)
        }
      }
  }
}
