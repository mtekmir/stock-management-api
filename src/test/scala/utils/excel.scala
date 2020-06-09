package utils

import com.merit.modules.excel.ExcelProductRow
import scala.util.Random
import ProductUtils._
import TestUtils._
import com.merit.modules.excel.ExcelStockOrderRow
import com.merit.modules.excel.ExcelWebSaleRow
import com.merit.modules.sales.SaleStatus
import com.merit.modules.products.Sku
import com.merit.modules.products.Barcode
import org.joda.time.DateTime
import com.merit.modules.products.Currency
import com.merit.modules.products.ProductRow
import com.merit.modules.sales.WebSaleSummary
import com.merit.modules.sales.WebSaleSummaryProduct
import com.merit.modules.excel.ExcelInventoryCountRow

object ExcelTestUtils {
  private def randomFrom(col: Seq[String]) = col.drop(Random.nextInt(col.size)).head
  def getExcelProductRows(
    n: Int,
    brand: Option[String] = None,
    category: Option[String] = None
  ): Seq[ExcelProductRow] =
    (1 to n)
      .map(
        i =>
          ExcelProductRow(
            randomBetween(10000000).toString,
            None,
            randomBetween(10000000).toString,
            randomProductName,
            randomPrice,
            randomDiscountPrice,
            randomQty,
            brand match {
              case None    => Some(randomBrandName)
              case Some(b) => Some(b)
            },
            category match {
              case None    => Some(randomCategoryName)
              case Some(c) => Some(c)
            },
            Some(randomBetween(20))
          )
      )
      .toSeq

  def excelProductRowToStockOrderRow(row: ExcelProductRow): ExcelStockOrderRow = {
    import row._
    ExcelStockOrderRow(
      name,
      sku,
      variation,
      barcode,
      qty,
      price,
      discountPrice,
      category,
      brand,
      taxRate
    )
  }

  def excelProductRowToInventoryCountRow(row: ExcelProductRow) = {
    import row._
    ExcelInventoryCountRow(
      name,
      sku,
      variation,
      barcode,
      qty,
      price,
      discountPrice,
      category,
      brand,
      taxRate
    )
  }

  def getExcelWebSaleRows(n: Int): Seq[ExcelWebSaleRow] =
    (1 to n).map(
      i =>
        ExcelWebSaleRow(
          randomBetween(10000).toString,
          Currency(randomBetween(10000)),
          Currency(randomBetween(100)),
          DateTime.now(),
          SaleStatus.trStatuses.toList(randomBetween(SaleStatus.trStatuses.size))._2,
          randomProductName,
          Some(Sku.random),
          randomBrandName,
          Some(Barcode.random),
          randomQty,
          randomPrice,
          randomBetween(20)
        )
    )

  implicit class Q1(
    val row: ExcelWebSaleRow
  ) {
    def toProductRow = {
      import row._
      ProductRow(
        barcode.getOrElse(""),
        sku.getOrElse(""),
        productName,
        price,
        None,
        qty,
        None,
        Some(tax),
        None,
        None
      )
    }

    def toExcelProductRow = {
      import row._
      ExcelProductRow(
        barcode.getOrElse(""),
        None,
        sku.getOrElse(""),
        productName,
        price,
        None,
        qty,
        Some(brand),
        None,
        Some(tax)
      )
    }

    def toSummary = {
      import row._
      WebSaleSummary(
        orderNo,
        total,
        discount,
        createdAt,
        status,
        Seq(WebSaleSummaryProduct(sku.get, barcode.get, qty))
      )
    }
  }
}
