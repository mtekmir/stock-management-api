package com.merit.external.crawler

import com.merit.modules.sales.SaleID
import com.merit.modules.products.ProductID
import com.merit.modules.stockOrders.StockOrderID
import com.merit.modules.inventoryCount.InventoryCountBatchID
import com.merit.modules.inventoryCount.InventoryCountProductID

object MessageType extends Enumeration {
  type MessageType = Value
  val Sale, StockOrder, InventoryCount = Value
}

case class SyncMessageProduct(
  id: ProductID,
  barcode: String,
  qty: Int  // Will be n if sold, -(n) if returned
)

case class SyncInventoryCountProduct(
  id: InventoryCountProductID,
  barcode: String,
  qty: Int
)

case class SyncResponseInventoryCountProduct(
  id: InventoryCountProductID,
  barcode: String,
  qty: Int,
  synced: Boolean
)

case class SyncResponseProduct(
  id: ProductID,
  barcode: String,
  qty: Int,
  synced: Boolean
)

case class SyncSaleMessage(
  saleId: SaleID,
  products: Seq[SyncMessageProduct]
)

case class SyncStockOrderMessage(
  stockOrderId: StockOrderID,
  products: Seq[SyncMessageProduct]
)

case class SyncInventoryCountMessage(
  inventoryCountBatchId: InventoryCountBatchID,
  products: Seq[SyncInventoryCountProduct]
)

case class SyncSaleResponse(
  saleId: SaleID,
  products: Seq[SyncResponseProduct]
)

case class SyncStockOrderResponse(
  stockOrderId: StockOrderID,
  products: Seq[SyncResponseProduct]
)

case class SyncInventoryCountResponse(
  inventoryCountBatchId: InventoryCountBatchID,
  products: Seq[SyncResponseInventoryCountProduct]
)