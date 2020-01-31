package com.merit.modules.stockOrders

import org.joda.time.DateTime
import slick.lifted.MappedTo
import com.merit.modules.products._
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow

case class StockOrderID(value: Long) extends AnyVal with MappedTo[Long]

case class StockOrderRow(
  date: DateTime,
  id: StockOrderID = StockOrderID(0L)
)

case class StockOrderDTOProduct(
  id: ProductID,
  barcode: String,
  sku: String,
  name: String,
  price: Currency,
  discountPrice: Option[Currency],
  qty: Int,
  variation: Option[String],
  taxRate: Option[Int],
  brand: Option[String],
  category: Option[String],
  synced: Boolean = false
)

object StockOrderDTOProduct {
  def fromRow(
    productRow: ProductRow,
    brand: Option[BrandRow] = None,
    category: Option[CategoryRow] = None,
    synced: Boolean
  ): StockOrderDTOProduct = {
    import productRow._
    StockOrderDTOProduct(
      id,
      barcode,
      sku,
      name,
      price,
      discountPrice,
      qty,
      variation,
      taxRate,
      brand.map(_.name),
      category.map(_.name),
      synced
    )
  }
}

case class StockOrderDTO(
  id: StockOrderID,
  createdAt: DateTime,
  products: Seq[StockOrderDTOProduct]
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