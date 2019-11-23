package utils

import com.merit.modules.excel.ExcelProductRow
import scala.util.Random
import ProductUtils._
import TestUtils._

object ExcelTestUtils {
  def getExcelProductRows(n: Int): Seq[ExcelProductRow] =
    (1 to n).map(
      i =>
        ExcelProductRow(
          randomBetween(10000000).toString,
          None,
          randomBetween(10000000).toString,
          randomProductName,
          randomPrice,
          randomQty,
          Some(randomBrandName),
          Some(randomCategoryName)
        )
    ).toSeq
}
