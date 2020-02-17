package api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.sales.SaleService
import com.merit.modules.products.ProductService
import scala.concurrent.ExecutionContext
import com.merit.modules.excel.ExcelService
import akka.http.scaladsl.server.Route
import com.merit.api.sales.{GetSaleTemplateRoute, SyncSaleRoute, GetSales, CreateSale}
import com.merit.modules.users.UserID
import com.merit.api.sales.ImportWebSalesRoute

object SaleRoutes extends Directives with JsonSupport {
  def apply(
    saleService: SaleService,
    excelService: ExcelService,
    userId: UserID
  )(
    implicit ec: ExecutionContext
  ): Route =
    pathPrefix("sales") {
      ImportSaleRoute(saleService, excelService, userId) ~
      ImportWebSalesRoute(saleService, excelService, userId) ~
      GetSale(saleService) ~
      GetSales(saleService) ~
      GetSaleTemplateRoute() ~
      CreateSale(saleService, userId)
    }
}
