package modules.excel

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import java.io.File
import com.merit.modules.excel._
import com.merit.modules.products.Currency

class ExcelServiceProductValidationSpec extends Specification {
  "Excel Service Product Import Validation" >> {
    import ValidationErrorTypes._
    import ExcelErrorMessages._
    "should return error for missing barcode" in new TestScope {
      val res = readFile("1missingbarcode1valid.xlsx")
      res must beEqualTo(
        Left(
          ExcelError(
            invalidProductImportMessage,
            Seq(
              ExcelValidationError(
                Seq(3),
                EmptyBarcodeError
              )
            )
          )
        )
      )
    }

    "should return error for missing barcode - 2" in new TestScope {
      val res = readFile("5missingbarcode1valid.xlsx")
      res must beEqualTo(
        Left(
          ExcelError(
            invalidProductImportMessage,
            Seq(
              ExcelValidationError(
                Seq(3, 4, 5, 6, 7),
                EmptyBarcodeError
              )
            )
          )
        )
      )
    }

    "should return error for missing fields" in new TestScope {
      val res = readFile("product-validation.xlsx")
      res.left.map(_.validationErrors.sortBy(_.errorType)) must beEqualTo(
        Left(
            Seq(
              ExcelValidationError(
                Seq(2, 3),
                DuplicateBarcodeError
              ),
              ExcelValidationError(
                Seq(3),
                EmptySkuError
              ),
              ExcelValidationError(
                Seq(5),
                InvalidBarcodeError
              ),
              ExcelValidationError(
                Seq(7),
                EmptyBarcodeError
              ),
              ExcelValidationError(
                Seq(4, 6),
                EmptyQtyError
              )
            ).sortBy(_.errorType)
        )
      )
    }

    "should return error for missing fields - 2" in new TestScope {
      val res = readFile("product-validation-2.xlsx")
      res.left.map(_.validationErrors.sortBy(_.errorType)) must beEqualTo(
        Left(
          Seq(
            ExcelValidationError(
              Seq(2, 5),
              InvalidBarcodeError
            ),
            ExcelValidationError(
              Seq(7,9),
              EmptyBarcodeError
            ),
            ExcelValidationError(
              Seq(6,10,11),
              DuplicateBarcodeError
            ),
            ExcelValidationError(
              Seq(3, 11),
              EmptySkuError
            ),
            ExcelValidationError(
              Seq(4, 6, 8, 10),
              EmptyQtyError
            )
          ).sortBy(_.errorType)
        )
      )
    }

    "should return error for missing fields - 3" in new TestScope {
      val res = readFile("product-validation-3.xlsx")
      res.left.map(_.validationErrors.sortBy(_.errorType)) must beEqualTo(
        Left(
          Seq(
            ExcelValidationError(
              Seq(2, 5),
              InvalidBarcodeError
            ),
            ExcelValidationError(
              Seq(9),
              EmptyBarcodeError
            ),
            ExcelValidationError(
              Seq(6, 7, 10, 11),
              DuplicateBarcodeError
            ),
            ExcelValidationError(
              Seq(3, 8),
              EmptySkuError
            ),
            ExcelValidationError(
              Seq(4, 6, 10, 11),
              EmptyQtyError
            )
          ).sortBy(_.errorType)
        )
      )
    }

    "should return error for missing fields - 4" in new TestScope {
      val res = readFile("product-validation-4.xlsx")
      res.left.map(_.validationErrors.sortBy(_.errorType)) must beEqualTo(
        Left(
          Seq(
            ExcelValidationError(
              Seq(2, 4),
              InvalidBarcodeError
            ),
            ExcelValidationError(
              Seq(8),
              EmptyBarcodeError
            ),
            ExcelValidationError(
              Seq(6, 7, 10, 11),
              DuplicateBarcodeError
            ),
            ExcelValidationError(
              Seq(3),
              EmptySkuError
            ),
            ExcelValidationError(
              Seq(6, 10, 11),
              EmptyQtyError
            )
          ).sortBy(_.errorType)
        )
      )
    }
  }

  class TestScope extends Scope {
    val testFilesPath = "src/test/resources/excel/products"
    val excelService  = ExcelService()
    val sampleProduct1 = ExcelProductRow(
      "32149023409",
      Some("US5"),
      "AO9303-394",
      "Nike Air Zoom Vapor",
      Currency.from("700.0"),
      4,
      Some("Nike"),
      Some("Erkek Ayakkabi")
    )
    def readFile(name: String) = {
      val file = new File(s"$testFilesPath/$name")
      excelService.parseProductImportFile(file)
    }
  }
}
