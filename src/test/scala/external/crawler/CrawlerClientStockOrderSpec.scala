package external.crawler
import org.specs2.mutable.Specification
import org.specs2.matcher.FutureMatchers
import org.scalamock.specs2.MockContext
import com.merit.external.sqsClient.SqsClient
import com.merit.external.crawler.CrawlerClient
import com.merit.CrawlerClientConfig
import org.specs2.concurrent.ExecutionEnv
import utils.ProductUtils._
import com.merit.modules.stockOrders.StockOrderSummary
import com.merit.modules.stockOrders.StockOrderID
import org.joda.time.DateTime

class CrawlerClientStockOrderSpec(implicit ee: ExecutionEnv)
    extends Specification
    with FutureMatchers {

  "Crawler Client" >> {
    "should send a message for stock order" in new TestScope {
      val res = crawlerClient.sendStockOrder(summary)

      res.map(_._1.products.map(p => (p.barcode, p.qty))) must beEqualTo(
        products.map(p => (p.barcode, 2))
      ).await
    }
  }

  class TestScope extends MockContext {
    val sqsClient = mock[SqsClient]
    val queueUrl  = "test"

    val crawlerClient = CrawlerClient(CrawlerClientConfig(queueUrl, "u", "p"), sqsClient)
    val products      = (1 to 25).map(_ => createProduct)

    (sqsClient.sendMessageTo _) expects (*, *, *)

    val summary = StockOrderSummary(
      StockOrderID(1L),
      DateTime.now,
      updated = products.map(p => productRowToStockOrderSummaryProduct(p, 2))
    )
  }
}
