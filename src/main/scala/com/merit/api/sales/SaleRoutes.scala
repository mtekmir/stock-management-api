package api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.sales.SaleService
import com.merit.modules.products.ProductService
import scala.concurrent.ExecutionContext
import com.merit.modules.excel.ExcelService
import akka.http.scaladsl.server.Route
import com.merit.api.sales.GetSaleTemplateRoute
import com.merit.api.sales.SyncSaleRoute
import com.merit.api.sales.GetSales

object SaleRoutes extends Directives with JsonSupport {
  def apply(
    saleService: SaleService,
    productService: ProductService,
    excelService: ExcelService
  )(
    implicit ec: ExecutionContext
  ): Route =
    pathPrefix("sales") {
      ImportSaleRoute(saleService, productService, excelService) ~
      GetSale(saleService) ~
      GetSales(saleService) ~
      GetSaleTemplateRoute() 
    }
}
