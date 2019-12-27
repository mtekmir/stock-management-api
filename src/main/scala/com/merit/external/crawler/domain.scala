package com.merit.external.crawler

import com.merit.modules.sales.SaleID
import com.merit.modules.products.ProductID
import com.merit.modules.stockOrders.StockOrderID

object AdjustmentType extends Enumeration {
  type AdjustmentType = Value
  val Decrease, Increase, NoChange = Value
}

case class SyncMessageProduct(
  id: ProductID,
  barcode: String,
  qty: Int, // adjustment type (increase) * quantity
  adjustmentType: AdjustmentType.Value
)

case class SyncSaleMessage(
  saleId: SaleID,
  products: Seq[SyncMessageProduct]
)

case class SyncStockOrderMessage(
  stockOrderId: StockOrderID,
  products: Seq[SyncMessageProduct]
)

case class SyncResponseProduct(
  id: ProductID,
  barcode: String,
  qty: Int,
  synced: Boolean
)

case class SyncSaleResponse(
  saleId: SaleID,
  products: Seq[SyncResponseProduct]
)