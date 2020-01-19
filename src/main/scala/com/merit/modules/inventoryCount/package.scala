package com.merit.modules

package object inventoryCount {

  import com.merit.modules.products.ProductDTO
  implicit class C2(
    val p: ProductDTO
  ) {
    def toInventoryCountDTOProduct: InventoryCountDTOProduct = {
      import p._
      InventoryCountDTOProduct(
        id,
        sku,
        barcode,
        name,
        variation,
        expected = qty,
        None,
        synced = false
      )
    }

    def toInventoryCountProductRow(
      batchId: InventoryCountBatchID
    ): InventoryCountProductRow = {
      import p._
      InventoryCountProductRow(
        batchId,
        id,
        expected = qty,
      )
    }
  }
}
