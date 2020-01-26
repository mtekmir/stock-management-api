package api

import akka.http.scaladsl.server.{Directives, Route}
import com.merit.modules.brands.BrandService
import com.merit.modules.products.ProductService
import com.merit.modules.excel.ExcelService
import scala.concurrent.ExecutionContext
import com.merit.modules.categories.CategoryService
import com.merit.api.products.{GetProducts, SearchProducts, GetProduct, EditProduct}
import com.merit.api.products.CreateProduct

object ProductRoutes extends Directives {
  def apply(
    productService: ProductService,
    excelService: ExcelService
  )(implicit ec: ExecutionContext): Route =
    pathPrefix("products") {
      ImportRoute(productService, excelService) ~
      GetProduct(productService) ~
      SearchProducts(productService) ~
      GetProducts(productService) ~
      EditProduct(productService) ~
      CreateProduct(productService)
    }
}
