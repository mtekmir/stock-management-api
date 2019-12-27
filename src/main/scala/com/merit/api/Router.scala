package api

import akka.http.scaladsl.server.Directives
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
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
import com.merit.api.sales.SyncSaleRoute
import akka.http.scaladsl.server.directives.Credentials
import com.merit.AppConfig

object Router extends Directives with AuthDirectives with JsonSupport {
  def apply(
    saleService: SaleService,
    productService: ProductService,
    brandService: BrandService,
    excelService: ExcelService,
    userService: UserService,
    categoryService: CategoryService,
    stockOrderService: StockOrderService,
    config: AppConfig
  )(implicit ec: ExecutionContext): Route = {
    def crawlerAuthenticator(credentials: Credentials): Option[Boolean] =
      credentials match {
        case c @ Credentials.Provided(config.crawlerClientConfig.username)
            if c.verify(config.crawlerClientConfig.password) =>
          Some(true)
        case _ => None
      }

    cors() {
      LoginRoute(userService, config.jwtConfig) ~
      SyncSaleRoute(saleService, crawlerAuthenticator) ~
      authenticated(config.jwtConfig) { userId =>
        ProductRoutes(brandService, productService, excelService, categoryService) ~
        SaleRoutes(saleService, productService, excelService) ~
        StockOrderRoutes(stockOrderService, excelService) ~
        UserRoutes(userId, userService)
      }
    }
  }
}
