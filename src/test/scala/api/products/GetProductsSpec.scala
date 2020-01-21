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
import com.merit.modules.products.PaginatedProductsResponse

class GetProductsSpec extends Specification with Specs2RouteTest with JsonSupport {
  "Get products route should call the correct method with parameter" >> {
    "brandId" in new TestScope {
      Get("/products?brandId=1") ~> productRoutes ~> check {
        status === StatusCodes.OK
        responseAs[PaginatedProductsResponse] === response
      }
    }

    "categoryId" in new TestScope {
      Get("/products?categoryId=2") ~> productRoutes ~> check {
        status === StatusCodes.OK
        responseAs[PaginatedProductsResponse] === response
      }
    }

    "page, rowsPerPage" in new TestScope {
      Get("/products?page=2&rowsPerPage=10") ~> productRoutes ~> check {
        status === StatusCodes.OK
        responseAs[PaginatedProductsResponse] === response
      }
    }

    "page, rowsPerPage, brandId, categoryId" in new TestScope {
      Get("/products?page=2&rowsPerPage=10&brandId=1&categoryId=1") ~> productRoutes ~> check {
        status === StatusCodes.OK
        responseAs[PaginatedProductsResponse] === response
      }
    }
  }

  class TestScope extends MockContext {
    val productService = mock[ProductService]
    val excelService   = mock[ExcelService]
    val products       = getExcelProductRows(5).map(excelRowToDTO(_))
    val response = PaginatedProductsResponse(5, products)

    (productService.getProducts _) expects (*, *, *) returning (Future(response))

    val productRoutes = ProductRoutes(productService, excelService)
  }
}


