package api.sales

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import akka.http.scaladsl.testkit.Specs2RouteTest
import api.JsonSupport
import com.merit.modules.excel.ExcelService
import com.merit.modules.sales.SaleRepo
import com.merit.modules.products.ProductRepo
import com.merit.modules.sales.SaleService
import org.scalamock.specs2.MockContext
import com.merit.external.crawler.CrawlerClient
import api.CreateSaleRequest
import com.merit.modules.products.Currency
import io.circe.syntax._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import com.merit.api.sales.CreateSale
import utils.ProductUtils._
import utils.ExcelTestUtils._
import com.merit.modules.products.ProductDTO
import com.merit.modules.sales.SaleSummary
import scala.concurrent.Future
import com.merit.modules.sales.SaleID
import org.joda.time.DateTime
import com.merit.modules.sales.SaleOutlet
import com.merit.modules.sales.SaleDTO
import com.merit.modules.sales.PaginatedSalesResponse
import com.merit.modules.users.UserID

class GetSalesRouteSpec extends Specification with Specs2RouteTest with JsonSupport {
  "Get sales route should call the correct method with parameter" >> {
    "empty" in new RouteScope {
      Get("/sales") ~> Route.seal(saleRoute) ~> check {
        status === StatusCodes.OK
      }

    }

    "startDate" in new RouteScope {
      Get("/sales?startDate=2019-12-29T23:00:00.000Z") ~> Route.seal(saleRoute) ~> check {
        status === StatusCodes.OK
      }
    }

    "endDate" in new RouteScope {
      Get("/sales?endDate=2019-12-29T23:00:00.000Z") ~> Route.seal(saleRoute) ~> check {
        status === StatusCodes.OK
      }
    }
  }

  class RouteScope extends MockContext {
    val crawlerClient = mock[CrawlerClient]
    val saleService   = mock[SaleService]
    val excelService  = mock[ExcelService]
    val total         = Currency(100.00)
    val discount      = Currency(10.37)
    val userId        = UserID.random
    val products      = getExcelProductRows(1).map(excelRowToSaleDTOProduct(_)).toSeq
    val response = PaginatedSalesResponse(
      1,
      Seq(SaleDTO(SaleID(1), DateTime.now(), SaleOutlet.Store, total, discount, products))
    )

    (saleService.getSales _) expects (*, *, *) returning (Future(response))

    val saleRoute = SaleRoutes(saleService, excelService, userId)
  }
}
