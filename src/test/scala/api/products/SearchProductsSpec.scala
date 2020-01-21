package api.products

import org.specs2.mutable.Specification
import akka.http.scaladsl.testkit.Specs2RouteTest
import org.scalamock.specs2.MockContext
import com.merit.modules.products.ProductService
import api.ProductRoutes
import com.merit.modules.excel.ExcelService
import akka.http.javadsl.model.StatusCodes
import com.merit.modules.products.ProductDTO
import api.JsonSupport
import scala.concurrent.Future
import utils.ProductUtils._
import utils.ExcelTestUtils._

class SearchProductsSpec extends Specification with Specs2RouteTest with JsonSupport {
  "Products search route" >> {
    "should return empty list when query is empty" in new TestScope {
      Get("/products/search/?q=") ~> productRoutes ~> check {
        status === StatusCodes.OK
        responseAs[List[ProductDTO]] === List()
      }
    }

    "should return the products for the query" in new TestScope {
      Get("/products/search/?q=N") ~> productRoutes ~> check {
        status === StatusCodes.OK
        responseAs[Seq[ProductDTO]] === products
      }
    }
  }

  class TestScope extends MockContext {
    val productService = mock[ProductService]
    val excelService   = mock[ExcelService]
    val products       = getExcelProductRows(5).map(excelRowToDTO(_))

    (productService.searchProducts _) expects (*) returning (Future(products))

    val productRoutes = ProductRoutes(productService, excelService)
  }
}
