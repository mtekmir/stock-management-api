package api

import akka.http.scaladsl.server.Directives
import com.merit.modules.sales.SaleService
import com.merit.modules.products.ProductService
import com.merit.modules.brands.BrandService
import com.merit.modules.excel.ExcelService
import scala.concurrent.ExecutionContext
import api.sales.SaleRoutes
import akka.http.scaladsl.server.Route
import com.merit.api.AuthDirectives
import com.merit.api.users.LoginRoute
import com.merit.modules.users.UserService
import com.merit.api.users.UserRoutes
import com.merit.modules.categories.CategoryService
import com.merit.api.stockOrders.StockOrderRoutes
import com.merit.modules.stockOrders.StockOrderService

object Router extends Directives with AuthDirectives with JsonSupport {
  def apply(
    saleService: SaleService,
    productService: ProductService,
    brandService: BrandService,
    excelService: ExcelService,
    userService: UserService,
    categoryService: CategoryService,
    stockOrderService: StockOrderService
  )(implicit ec: ExecutionContext): Route =
    authenticated { userId =>
      ProductRoutes(brandService, productService, excelService, categoryService) ~
      SaleRoutes(saleService, productService, excelService) ~
      StockOrderRoutes(stockOrderService, excelService) ~
      UserRoutes(userId, userService)
    } ~ LoginRoute(userService)
}
