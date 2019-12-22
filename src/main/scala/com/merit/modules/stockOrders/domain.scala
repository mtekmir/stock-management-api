package com.merit.modules.stockOrders

import org.joda.time.DateTime
import slick.lifted.MappedTo
import com.merit.modules.products._

case class StockOrderID(value: Long) extends AnyVal with MappedTo[Long]

case class StockOrderRow(
  date: DateTime,
  id: StockOrderID = StockOrderID(0L)
)

case class StockOrderDTO(
  id: StockOrderID,
  createdAt: DateTime,
  products: Seq[ProductDTO]
)

case class StockOrderSummaryProduct(
  id: ProductID,
  barcode: String,
  name: String,
  variation: Option[String] = None,
  prevQty: Int = 0,
  ordered: Int
)

object StockOrderSummaryProduct {
  def fromProductDTO(p: ProductDTO, prevQty: Int = 0, ordered: Int): StockOrderSummaryProduct = {
    import p._
    StockOrderSummaryProduct(id, barcode, name, variation, prevQty, ordered)
  } 

  def fromProductRow(p: ProductRow): StockOrderSummaryProduct = {
    import p._
    StockOrderSummaryProduct(id, barcode, name, variation, prevQty = 0, qty)
  } 
}

case class StockOrderSummary(
  id: StockOrderID,
  date: DateTime,
  created: Seq[StockOrderSummaryProduct] = Seq(),
  updated: Seq[StockOrderSummaryProduct] = Seq()
)