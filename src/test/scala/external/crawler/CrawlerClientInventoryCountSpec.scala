package external.crawler

import org.specs2.mutable.Specification
import org.specs2.matcher.FutureMatchers
import org.scalamock.specs2.MockContext
import com.merit.external.sqsClient.SqsClient
import com.merit.external.crawler.CrawlerClient
import com.merit.CrawlerClientConfig
import org.specs2.concurrent.ExecutionEnv
import utils.ProductUtils._
import org.joda.time.DateTime
import com.merit.modules.inventoryCount.InventoryCountDTO
import com.merit.modules.inventoryCount.InventoryCountBatchID
import com.merit.modules.inventoryCount.InventoryCountStatus
import com.merit.modules.inventoryCount.InventoryCountProductDTO
import com.merit.external.crawler.SyncInventoryCountMessage
import com.merit.external.crawler.SyncMessageProduct
import com.merit.modules.inventoryCount.InventoryCountProductID
import com.merit.modules.products.ProductID
import com.merit.external.crawler.SyncInventoryCountProduct

class CrawlerClientInventoryCountSpec(implicit ee: ExecutionEnv)
    extends Specification
    with FutureMatchers {

  "Crawler Client" >> {
    "should send correct inventory count messages to crawler" in new TestScope {
      val res = crawlerClient.sendInventoryCount(summary, batchProducts)

      res.map(_._1) must beEqualTo(
        List(
          SyncInventoryCountMessage(
            batchId,
            products = batchProducts.map(
              p => SyncInventoryCountProduct(p.id, p.barcode, p.counted.getOrElse(0))
            )
          )
        )
      ).await
    }
  }

  class TestScope extends MockContext {
    val sqsClient = mock[SqsClient]
    val queueUrl  = "test"

    val crawlerClient = CrawlerClient(CrawlerClientConfig(queueUrl, "u", "p"), sqsClient)
    val products      = (1 to 25).map(_ => createProduct)

    (sqsClient.sendMessageTo _) expects (*, *, *)
    val batchId = InventoryCountBatchID(1L)

    val summary = InventoryCountDTO(
      batchId,
      InventoryCountStatus.Completed,
      DateTime.now,
      Some(DateTime.now()),
      None,
      None,
      None
    )

    val batchProducts = products.map(
      p =>
        InventoryCountProductDTO(
          InventoryCountProductID(p.id.value),
          p.sku,
          p.barcode,
          p.name,
          p.variation,
          p.qty,
          DateTime.now(),
          Some(p.qty + 1),
          false,
          false
        )
    )
  }
}
