package modules.excel

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import com.merit.modules.excel.ExcelService
import java.io.File
import com.merit.modules.excel.ExcelSaleRow

class ExcelServiceSaleSpec extends Specification {
  "Excel Service" >> {
    "should read all the lines" in new TestScope {
      val res = readFile("sample-sale.xlsx")
      res.right.map(_.sortBy(_.barcode)) must beEqualTo(
        Right(
          Seq(
            product.copy(barcode = "93894893894"),
            product.copy(barcode = "93894893233"),
            product.copy(barcode = "938948938123"),
            product.copy(barcode = "1231231233", qty = 2),
            product.copy(barcode = "89489383982")
          ).sortBy(_.barcode)
        )
      )
    }

    "should combine the quantities of the same barcodes" in new TestScope {
      val res = readFile("sale-same-barcode.xlsx")
      res must beEqualTo(
        Right(
          Seq(product.copy(barcode = "93894893233", qty = 6))
        )
      )
    }

    "should take account negative qtys" in new TestScope {
      val res = readFile("sale-negative-qtys.xlsx")

      res.right.map(_.sortBy(_.barcode)) must beEqualTo(
        Right(
          Seq(
            product.copy(barcode = "93894893233", qty = -1),
            product.copy(barcode = "9389489323323"),
            product.copy(barcode = "938948932332"),
            product.copy(barcode = "93894892332", qty = 2),
            product.copy(barcode = "938948931221", qty = 10),
            product.copy(barcode = "938948931223"),
            product.copy(barcode = "9389489323444", qty = -12)
          ).sortBy(_.barcode)
        )
      )
    }

    "should calculate correct quantities" in new TestScope {
      val res = readFile("sale-negative-qtys-1.xlsx")

      res must beEqualTo(Right(Seq(product.copy(barcode = "93894893233", qty = -2))))
    }

    "should read sale import file" in new TestScope {
      val res = readFile("sample-sale-1.xlsx")

      res.right.map(_.sortBy(_.barcode)) must beEqualTo(
        Right(
          Seq(
            product.copy(barcode = "93894893233", qty = -3),
            product.copy(barcode= "8398398398398"),
            product.copy(barcode="8839383989938", qty = -1),
            product.copy(barcode="88393893893"),
            product.copy(barcode="382329382932", qty = -1),
            product.copy(barcode="9283923829382"),
          ).sortBy(_.barcode)
        )
      )
    }
  }

  class TestScope extends Scope {
    val testFilesPath = "src/test/resources/excel/sales"
    val excelService  = ExcelService()
    val product       = ExcelSaleRow("12312", 1)
    def readFile(name: String) = {
      val file = new File(s"$testFilesPath/$name")
      excelService.parseSaleImportFile(file)
    }
  }
}
