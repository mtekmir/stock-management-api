package com.merit.modules.products

import slick.lifted.MappedTo
import java.util.UUID
import com.merit.modules.stockOrders.StockOrderID
import com.merit.modules.brands.{BrandID, BrandRow}
import com.merit.modules.sales.SaleID

case class ProductID(value: Long) extends AnyVal with MappedTo[Long]

final case class ProductRow(
  barcode: String,
  sku: String,
  name: String,
  price: Double,
  qty: Int,
  variation: String,
  brandId: BrandID,
  id: ProductID = ProductID(0)
) 

final case class SoldProductRow(
  productId: ProductID,
  saleId: SaleID,
  qty: Int = 1,
  id: Long = 0L
) 

final case class OrderedProductRow(
  productId: ProductID,
  stockOrderId: StockOrderID,
  qty: Int = 1,
  id: Long = 0L
)

final case class ProductDTO(
  id: ProductID,
  barcode: String,
  sku: String,
  name: String,
  price: Double,
  qty: Int,
  variation: String,
  brand: String
)

object ProductDTO {
  def fromRow(productRow: ProductRow, brand: BrandRow): ProductDTO = {
    import productRow._
    ProductDTO(id, barcode, sku, name, price, qty, variation, brand.name)
  }
}
