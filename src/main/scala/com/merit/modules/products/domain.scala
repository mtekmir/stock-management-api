package com.merit.modules.products

import slick.lifted.MappedTo
import java.util.UUID
import com.merit.modules.stockOrders.StockOrderID
import com.merit.modules.brands.{BrandID, BrandRow}
import com.merit.modules.sales.SaleID
import com.merit.modules.categories.CategoryID
import com.merit.modules.categories.CategoryRow
import com.merit.modules.excel.ExcelStockOrderRow
import com.merit.modules.excel.ExcelProductRow

case class ProductID(value: Long) extends AnyVal with MappedTo[Long]

object ProductID {
  def zero: ProductID = ProductID(0L)
}

final case class ProductRow(
  barcode: String,
  sku: String,
  name: String,
  price: Double,
  qty: Int,
  variation: Option[String],
  brandId: Option[BrandID],
  categoryId: Option[CategoryID],
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
  variation: Option[String],
  brand: Option[String],
  category: Option[String]
)

object ProductDTO {
  def fromRow(productRow: ProductRow, brand: Option[BrandRow] = None, category: Option[CategoryRow] = None): ProductDTO = {
    import productRow._
    ProductDTO(id, barcode, sku, name, price, qty, variation, brand.map(_.name), category.map(_.name))
  }
}
