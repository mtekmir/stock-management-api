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

    // val a = Right(Vector(ExcelStockOrderRow("","",None,"3324921648019",9,None,None,None), ExcelStockOrderRow("","",None,"3324921739151",3,None,None,None), ExcelStockOrderRow("","",None,3324921320229,12,None,None,None), ExcelStockOrderRow(LOONY DAMP X 2,700034-184,None,3324921320236,200,Some(399.979934),Some(Titresim Onleyici),Some(Babolat)), ExcelStockOrderRow(LOONY DAMP X 2,700009-141,None,3324920150872,1,Some(39.99),Some(Titresim Onleyici),Some(Babolat)), ExcelStockOrderRow(OVER GRIP VS ORIGINAL X 3 BLACK,653040-105,None,3324921393841,2,Some(799.34),Some(Grip),Some(Babolat)), ExcelStockOrderRow(TRACTION X 3 ASSORTED,653043-134,None,3324921393629,3,None,Some(Grip),Some(Babolat)), ExcelStockOrderRow(RPM BLAST TEKLİ,241101-105,None,3324921176437,30,None,Some(Kordaj),Some(Babolat)), ExcelStockOrderRow(GEL SOLUTION SPEED 3 KADIN Cherry Clay,"E551Y-0133",Some(US6),4549846707231,1,None,Some(Kadin Ayakkabi),Some(Asics))))
    
    // val b = Right(List(ExcelStockOrderRow("","",None,3324921648019,9,None,None,None), ExcelStockOrderRow(,,None,3324921739151,3,None,None,None), ExcelStockOrderRow(,,None,3324921320229,12,None,None,None), ExcelStockOrderRow(LOONY DAMP X 2,700034-184,None,3324921320236,200,Some(39.99),Some(Titresim Onleyici),Some(Babolat)), ExcelStockOrderRow(LOONY DAMP X 2,700009-141,None,3324920150872,1,Some(39.99),Some(Titresim Onleyici),Some(Babolat)), ExcelStockOrderRow(OVER GRIP VS ORIGINAL X 3 BLACK,653040-105,None,3324921393841,2,Some(39.99),Some(Grip),Some(Babolat)), ExcelStockOrderRow(TRACTION X 3 ASSORTED,653043-134,None,3324921393629,3,Some(39.99),Some(Grip),Some(Babolat)), ExcelStockOrderRow(RPM BLAST TEKLİ,241101-105,None,3324921176437,30,Some(109.99),Some(Kordaj),Some(Babolat)), ExcelStockOrderRow(GEL SOLUTION SPEED 3 KADIN Cherry Clay,"E551Y-0133",Some(US6),4549846707231,1,Some(499.99),Some(Kadin Ayakkabi),Some(Asics))))
  }
}
