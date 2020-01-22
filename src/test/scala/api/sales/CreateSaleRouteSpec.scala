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
import com.merit.modules.products.ProductDTO
import com.merit.modules.sales.SaleSummary
import scala.concurrent.Future
import com.merit.modules.sales.SaleID
import org.joda.time.DateTime
import com.merit.modules.sales.SaleOutlet

class SalesRoutesSpec extends Specification with Specs2RouteTest with JsonSupport {
  "Sale route" >> {
    "should return error when no products in create sale req" in new RouteScope {
      val createSaleReq = CreateSaleRequest(total, discount, Seq())
      Post("/sales", createSaleReq.asJsonObject) ~> Route.seal(saleRoute) ~> check {
        responseAs[String] === "Products must not be empty"
        status === StatusCodes.BadRequest
      }

      "should return ok with summary" in new RouteScope {
        val createSaleReq = CreateSaleRequest(total, discount, Seq(rowToDTO(createProduct)))
        Post("/sales", createSaleReq.asJsonObject) ~> saleRoute ~> check {
          status === StatusCodes.OK
          val summary = responseAs[SaleSummary]
          summary.total === createSaleReq.total
          summary.discount === createSaleReq.discount
          summary.products === products
        }
      }
    }
  }

  class RouteScope extends MockContext {
    val crawlerClient = mock[CrawlerClient]
    val saleService   = mock[SaleService]
    val excelService  = mock[ExcelService]
    val total         = Currency(100.00)
    val discount      = Currency(10.37)
    val products      = Seq(productRowToSaleSummaryProduct(createProduct, 5))
    (saleService.create _) expects (*, *, *) returning (Future(
      SaleSummary(SaleID(1), DateTime.now(), total, discount, SaleOutlet.Store, products)
    ))

    val saleRoute = SaleRoutes(saleService, excelService)
  }
}
