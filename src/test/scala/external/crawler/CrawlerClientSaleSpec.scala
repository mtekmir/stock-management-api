package external.crawler

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.Scope
import org.scalamock.specs2.MockContext
import com.merit.external.crawler.CrawlerClient
import com.merit.external.sqsClient.SqsClient
import com.merit.CrawlerClientConfig
import com.merit.modules.sales.SaleSummary
import com.merit.modules.sales.SaleID
import utils.ProductUtils._
import com.merit.modules.sales.SaleSummaryProduct
import org.specs2.mutable.Specification
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.merit.external.crawler.CrawlerCodec
import io.circe.parser._
import com.merit.external.crawler.SyncSaleMessage
import com.merit.external.crawler.AdjustmentType
import com.merit.modules.products.ProductRow

class CrawlerClientSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers {

  "Crawler client" >> {
    "convert sale summary to message - 1" in new TestScope {
      val res = crawlerClient.sendSale(summary1)

      res.map(_._1.products.map(p => (p.barcode, p.qty))) must beEqualTo(
        products.map(p => (p.barcode, 1))
      ).await
    }

    "convert sale summary to message - 2" in new TestScope {
      val res = crawlerClient.sendSale(summary2)

      res.map(_._1.products.map(p => (p.barcode, p.qty))) must beEqualTo(
        products.map(p => (p.barcode, -1))
      ).await
    }

    "convert sale summary to message - 3" in new TestScope {
      val res = crawlerClient.sendSale(summary3)

      res.map(_._1.products.map(p => (p.barcode, p.qty))) must beEqualTo(
        products.map(p => (p.barcode, 0))
      ).await
    }

    "convert sale summary to message - 4" in new TestScope {
      val res = crawlerClient.sendSale(summary4)

      res.map(_._1.products.map(p => (p.barcode, p.qty))) must beEqualTo(
        products.sortBy(_.barcode).take(8).zip(qtys).map {
          case (p, q) => (p.barcode, q)
        }
      ).await
    }
  }

  class TestScope extends MockContext {
    import AdjustmentType._
    val sqsClient = mock[SqsClient]
    val queueUrl  = "test"

    val crawlerClient = CrawlerClient(CrawlerClientConfig(queueUrl, "u", "p"), sqsClient)
    val products      = (1 to 25).map(_ => createProduct)

    (sqsClient.sendMessageTo _) expects (*, *)
    val summary1 = SaleSummary(
      SaleID(1L),
      products.map(p => productRowToSaleSummaryProduct(p, 1))
    )

    val summary2 = SaleSummary(
      SaleID(0L),
      products.map(p => productRowToSaleSummaryProduct(p, -1))
    )

    val summary3 = SaleSummary(
      SaleID(0L),
      products.map(p => productRowToSaleSummaryProduct(p, 0))
    )

    val qtys = Seq(0, 1, 2, -1, -3, 0, -5, 4)

    val summary4 = SaleSummary(SaleID(1L), products.sortBy(_.barcode).take(8).zip(qtys).map {
      case (p, q) => productRowToSaleSummaryProduct(p, q)
    })
  }
}
