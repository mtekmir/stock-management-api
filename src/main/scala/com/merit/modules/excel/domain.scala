package com.merit.modules.excel
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID
import com.merit.modules.brands.BrandRow

sealed trait ExcelRow

case class ExcelProductRow(
  barcode: String,
  variation: Option[String],
  sku: String,
  name: String,
  price: Double,
  qty: Int,
  brand: Option[String],
  category: Option[String]
) extends ExcelRow

object ExcelProductRow {
  def toProductRow(row: ExcelProductRow, brandId: Option[BrandID], categoryId: Option[CategoryID]): ProductRow = {
    import row._
    ProductRow(barcode, sku, name, price, qty, variation, brandId, categoryId)
  }
}

case class DuplicateBarcode(
  barcode: String,
  rowIndex: Seq[Int]
)

case class ExcelSaleRow(
  barcode: String,
  qty: Int = 1
) extends ExcelRow

case class ExcelStockOrderRow(
  name: String,
  sku: String,
  variation: Option[String],
  barcode: String,
  qty: Int,
  price: Double,
  category: Option[String],
  brand: Option[String]
) extends ExcelRow 

object ExcelStockOrderRow {
  def toProductRow(row: ExcelStockOrderRow, brandId: Option[BrandID], categoryId: Option[CategoryID]): ProductRow = {
    import row._

    ProductRow(barcode, sku, name, price, qty, variation, brandId, categoryId)
  }
}