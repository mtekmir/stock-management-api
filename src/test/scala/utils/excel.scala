package utils

import com.merit.modules.excel.ExcelProductRow
import scala.util.Random
import ProductUtils._
import TestUtils._
import com.merit.modules.excel.ExcelStockOrderRow

object ExcelTestUtils {
  def getExcelProductRows(n: Int): Seq[ExcelProductRow] =
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
            Some(randomBrandName),
            Some(randomCategoryName),
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
}
