package modules.excel
import org.specs2.specification.Scope
import java.io.File
import com.merit.modules.excel.ExcelService
import org.specs2.mutable.Specification
import com.merit.modules.excel.ExcelProductRow
import com.merit.modules.products.Currency

class ExcelServiceSpec extends Specification {
  "Excel service" >> {
    "should read product rows" in new TestScope {
      val products = readFile("10products.xlsx")
      products.map {
        _.map {
          case ExcelProductRow(_, _, _, _, _, _, _, _, _, _) => 1
        }
      }.map(_.sum) must beEqualTo(Right(10))
    }

    "should read 1 product row correctly" in new TestScope {
      val products = readFile("1product.xlsx")
      products must beEqualTo(Right(Seq(sampleProduct1)))
    }

    "should read 1 product row correctly" in new TestScope {
      val products = readFile("1empty1product.xlsx")
      products must beEqualTo(Right(Seq(sampleProduct1)))
    }

    "should read products correctly" in new TestScope {
      val products = readFile("sample-product-import.xlsx")
      products must beEqualTo(
        Right(
          Seq(
            sampleProduct1.copy(
              name = "Nike 1",
              sku = "AO9303-393",
              discountPrice = Currency.from("499.89"),
              taxRate = Some(8)
            ),
            sampleProduct1
              .copy(
                barcode = "32149023411",
                name = "Nike 2",
                discountPrice = Currency.from("399.11"),
                taxRate = Some(8)
              ),
            sampleProduct1.copy(
              barcode = "32149023415",
              name = "Nike 3",
              qty = 11,
              discountPrice = Some(Currency(299.90)),
              taxRate = Some(18)
            ),
            sampleProduct1
              .copy(
                barcode = "3214902341212",
                sku = "AJ9303-395",
                name = "Nike Air",
                discountPrice = Some(Currency(100.99)),
                taxRate = Some(20)
              ),
            sampleProduct1.copy(
              barcode = "3214902342",
              sku = "AO9303-397",
              name = "Nike Zoom",
              price = Currency.from("100.22"),
              category = Some("Men's Shoe"),
              discountPrice = Some(Currency(299.01)),
              taxRate = Some(8)
            ),
            sampleProduct1.copy(
              barcode = "32149023410",
              sku = "AO9303-394",
              name = "Nike D",
              qty = 22,
              discountPrice = Some(Currency(920.29)),
              taxRate = Some(10)
            ),
            sampleProduct1.copy(
              barcode = "32149023412",
              sku = "E550-711Y",
              name = "Asics 1",
              qty = 34,
              price = Currency.from("888.99"),
              category = Some("Women's Shoe"),
              brand = Some("Asics"),
              discountPrice = Some(Currency(919.91)),
              taxRate = Some(9)
            ),
            sampleProduct1.copy(
              barcode = "321490234123",
              variation = None,
              sku = "AO9303-396",
              name = "Nike v",
              qty = 1,
              category = None,
              brand = None,
              price = None
            ),
            sampleProduct1.copy(
              barcode = "3214902341632",
              variation = None,
              sku = "AO9303-397",
              name = "Nike AA",
              qty = 1,
              category = None,
              brand = None,
              price = None
            ),
            sampleProduct1.copy(
              barcode = "321490234121",
              variation = None,
              sku = "AO9303-398",
              name = "Nike Air Zoom Vapor",
              qty = 1,
              category = None,
              brand = None,
              price = None
            )
          )
        )
      )
    }

    "Should parse prices correctly" in new TestScope {
      val res = readFile("product-prices.xlsx")

      res must beEqualTo(
        Right(
          Seq(
            sampleProduct1,
            sampleProduct1
              .copy(barcode = "32149023411", name = "Nike 2", price = Some(Currency(700.89))),
            sampleProduct1.copy(
              barcode = "32149023415",
              name = "Nike 3",
              price = Some(Currency(700.99)),
              qty = 11
            ),
            sampleProduct1.copy(
              barcode = "3214902341212",
              name = "Nike Air",
              price = Some(Currency(700.01)),
              sku = "AJ9303-395"
            ),
            sampleProduct1.copy(
              barcode = "3214902342",
              name = "Nike Zoom",
              price = Some(Currency(100.22)),
              sku = "AO9303-397"
            ),
            sampleProduct1.copy(
              barcode = "32149023410",
              name = "Nike D",
              price = Some(Currency(700.00)),
              sku = "AO9303-394",
              qty = 22
            ),
            sampleProduct1.copy(
              barcode = "32149023412",
              name = "Asics 1",
              sku = "E550-711Y",
              price = Some(Currency(888.99)),
              qty = 34
            ),
            sampleProduct1.copy(
              barcode = "321490234123",
              name = "Nike v",
              sku = "AO9303-396",
              price = Some(Currency(88.00)),
              qty = 1,
              variation = None
            ),
            sampleProduct1.copy(
              barcode = "3214902341632",
              name = "Nike AA",
              sku = "AO9303-397",
              price = Some(Currency(10000.99)),
              qty = 1,
              variation = None
            ),
            sampleProduct1.copy(
              barcode = "321490234121",
              name = "Nike Air Zoom Vapor",
              sku = "AO9303-398",
              price = Some(Currency(10.10)),
              qty = 1,
              variation = None
            )
          )
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
      Currency.from("700.00"),
      None,
      4,
      Some("Nike"),
      Some("Erkek Ayakkabi"),
      None
    )
    def readFile(name: String) = {
      val file = new File(s"$testFilesPath/$name")
      excelService.parseProductImportFile(file)
    }
  }
}
