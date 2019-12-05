package modules.excel

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import com.merit.modules.excel.ExcelService
import java.io.File
import com.merit.modules.excel._

class ExcelServiceStockOrderValidationSpec extends Specification {
  import ValidationErrorTypes._
    import ExcelErrorMessages._
  "Excel service stock order input validation" >> {
    "should return errors for invalid rows" in new TestScope {
      val res = readFile("stock-order-validation.xlsx")
      res.left.map(_.validationErrors.sortBy(_.errorType)) must beEqualTo(
        Left(
          Seq(
            ExcelValidationError(
              Seq(2, 4, 9, 13, 16, 18, 20),
              EmptyBarcodeError
            ),
            ExcelValidationError(
              Seq(6, 8, 11, 14, 17, 21),
              EmptyQtyError
            )
          ).sortBy(_.errorType)
        )
      )
    }
    
    "should return errors for invalid rows - 2" in new TestScope {
      val res = readFile("stock-order-validation-1.xlsx")
      res.left.map(_.validationErrors.sortBy(_.errorType)) must beEqualTo(
        Left(
          Seq(
            ExcelValidationError(
              Seq(6, 8 , 11, 16, 19),
              EmptyBarcodeError
            ),
            ExcelValidationError(
              Seq(7, 9, 17, 18, 20, 23),
              EmptyQtyError
            ),
            ExcelValidationError(
              Seq(13, 22),
              InvalidQtyError
            )
          ).sortBy(_.errorType)
        )
      )
    }

    "should return errors for invalid rows - 3" in new TestScope {
      val res = readFile("stock-order-validation-2.xlsx")
      res.left.map(_.validationErrors.sortBy(_.errorType)) must beEqualTo(
        Left(
          Seq(
            ExcelValidationError(
              Seq(13, 14, 18, 19),
              EmptyBarcodeError
            ),
            ExcelValidationError(
              Seq(7, 9, 16, 21),
              EmptyQtyError
            ),
            ExcelValidationError(
              Seq(8,10),
              InvalidQtyError
            ),
            ExcelValidationError(
              Seq(22, 23),
              InvalidBarcodeError
            )
          ).sortBy(_.errorType)
        )
      )
    }
  }

  class TestScope extends Scope {
    val testFilesPath = "src/test/resources/excel/stockOrders"
    val excelService  = ExcelService()
    def readFile(name: String) = {
      val file = new File(s"$testFilesPath/$name")
      excelService.parseStockOrderImportFile(file)
    }
  }
}
