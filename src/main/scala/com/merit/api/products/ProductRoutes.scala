package api
import akka.http.scaladsl.server.Directives
import com.merit.modules.brands.BrandService
import com.merit.modules.products.ProductService
import com.merit.modules.excel.ExcelService
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Route
import com.merit.api.products.GetProduct
import com.merit.modules.categories.CategoryService
import com.merit.api.products.GetProducts

object ProductRoutes extends Directives {
  def apply(
    productService: ProductService,
    excelService: ExcelService,
  )(implicit ec: ExecutionContext): Route =
    pathPrefix("products") {
      ImportRoute(productService, excelService) ~
      GetProduct(productService) ~
      GetProducts(productService)
    }
}
