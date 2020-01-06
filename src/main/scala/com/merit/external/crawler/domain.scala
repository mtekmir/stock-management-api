package com.merit.external.crawler

import com.merit.modules.sales.SaleID
import com.merit.modules.products.ProductID
import com.merit.modules.stockOrders.StockOrderID

object MessageType extends Enumeration {
  type MessageType = Value
  val Sale, StockOrder = Value
}

case class SyncMessageProduct(
  id: ProductID,
  barcode: String,
  qty: Int  // Will be n if sold, -(n) if returned
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

case class SyncSaleResponse(
  saleId: SaleID,
  products: Seq[SyncResponseProduct]
)

case class SyncStockOrderResponse(
  stockOrderId: StockOrderID,
  products: Seq[SyncResponseProduct]
)

