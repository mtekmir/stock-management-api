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
import scala.util.Try
import scala.math.BigDecimal.RoundingMode

case class ProductID(value: Long) extends AnyVal with MappedTo[Long]

object ProductID {
  def zero: ProductID = ProductID(0L)
}

case class Currency(value: BigDecimal)

object Currency {
  def format(c: BigDecimal): BigDecimal = c.setScale(2, RoundingMode.DOWN)
  def isValid(input: String): Boolean   = Try(BigDecimal(input)).isSuccess

  def from(input: String): Option[Currency] =
    Try(BigDecimal(input)).toOption.map(c => Currency(format(c)))
  def fromDb(input: BigDecimal): Currency = Currency(format(BigDecimal(input.toString)))
}

final case class ProductRow(
  barcode: String,
  sku: String,
  name: String,
  price: Option[Currency],
  discountPrice: Option[Currency],
  qty: Int,
  variation: Option[String],
  taxRate: Option[Int],
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
  price: Option[Currency],
  discountPrice: Option[Currency],
  qty: Int,
  variation: Option[String],
  taxRate: Option[Int],
  brand: Option[String],
  category: Option[String]
)

object ProductDTO {
  def fromRow(
    productRow: ProductRow,
    brand: Option[BrandRow] = None,
    category: Option[CategoryRow] = None
  ): ProductDTO = {
    import productRow._
    ProductDTO(
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
      category.map(_.name)
    )
  }

  def toRow(
    dto: ProductDTO,
    brandId: Option[BrandID],
    categoryId: Option[CategoryID]
  ): ProductRow = {
    import dto._
    ProductRow(
      barcode,
      sku,
      name,
      price,
      discountPrice,
      qty,
      variation,
      taxRate,
      brandId,
      categoryId,
      id
    )
  }
}
