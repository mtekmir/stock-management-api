package modules.excel

import org.specs2.mutable.Specification
import com.merit.modules.excel.ExcelService
import org.specs2.specification.Scope
import java.io.File
import com.merit.modules.excel._

class ExcelServiceWebSalesValidationSpec extends Specification {
  import ValidationErrorTypes._
    import ExcelErrorMessages._

  "Excel Service web sales import validation" >> {
    "should return errors for invalid rows" in new TestScope {
      val res = readFile("web_sales_validation_1.xlsx")

      res.left.map(_.validationErrors.sortBy(_.errorType)) must beEqualTo(
        Left(
          Seq(
            ExcelValidationError(
              Seq(10),
              InvalidBarcodeError
            ),
            ExcelValidationError(
              Seq(2, 6, 11),
              InvalidPriceError
            ),
            ExcelValidationError(
              Seq(4),
              InvalidDateError
            ),
            ExcelValidationError(
              Seq(5),
              InvalidStatusError
            ),
            ExcelValidationError(
              Seq(7),
              InvalidTaxRateError
            ),
            ExcelValidationError(
              Seq(8,9),
              InvalidOrderNoError
            )
          ).sortBy(_.errorType)
        )
      )
    }
  }

  class TestScope extends Scope {
    val testFilesPath = "src/test/resources/excel/webSales"
    val excelService  = ExcelService()
    def readFile(name: String) = {
      val file = new File(s"$testFilesPath/$name")
      excelService.parseWebSaleImportFile(file)
    }
  }
}