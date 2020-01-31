package modules.excel
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import com.merit.modules.excel.ExcelService
import java.io.File
import com.merit.modules.excel.ExcelStockOrderRow
import com.merit.modules.products.Currency

class ExcelServiceStockOrderSpec extends Specification {
  "Excel Service" >> {
    "should read stock order rows" in new TestScope {
      val products = readFile("stock-order-sample.xlsx")

      products must beEqualTo(
        Right(
          Seq(
            onlyBarcodeProduct,
            onlyBarcodeProduct.copy(barcode = "3324921739151", qty = 3),
            onlyBarcodeProduct.copy(barcode = "3324921320229", qty = 12),
            fullProduct,
            fullProduct.copy(sku = "700009-141", barcode = "3324920150872", qty = 1),
            fullProduct.copy(
              name = "OVER GRIP VS ORIGINAL X 3 BLACK",
              sku = "653040-105",
              barcode = "3324921393841",
              qty = 2,
              category = Some("Grip")
            ),
            fullProduct.copy(
              name = "TRACTION X 3 ASSORTED",
              sku = "653043-134",
              barcode = "3324921393629",
              qty = 3,
              category = Some("Grip")
            ),
            fullProduct.copy(
              name = "RPM BLAST TEKLİ",
              sku = "241101-105",
              barcode = "3324921176437",
              qty = 30,
              price = Currency.fromOrZero("109.99"),
              category = Some("Kordaj")
            ),
            fullProduct.copy(
              name = "GEL SOLUTION SPEED 3 KADIN Cherry Clay",
              sku = "E551Y-0133",
              variation = Some("US6"),
              barcode = "4549846707231",
              qty = 1,
              price = Currency.fromOrZero("499.99"),
              category = Some("Kadin Ayakkabi"),
              brand = Some("Asics")
            )
          )
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

    val onlyBarcodeProduct =
      ExcelStockOrderRow("", "", None, "3324921648019", 9, Currency(0), None, None, None, None)
    val fullProduct = ExcelStockOrderRow(
      "LOONY DAMP X 2",
      "700034-184",
      None,
      "3324921320236",
      200,
      Currency.fromOrZero("39.99"),
      None,
      Some("Titresim Onleyici"),
      Some("Babolat"),
      None
    )
  }
}
