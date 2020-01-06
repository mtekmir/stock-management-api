package modules.stockOrders

import db.ServiceSpec
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.Scope
import com.merit.modules.brands.BrandRepo
import com.merit.modules.products.ProductRepo
import org.specs2.concurrent.ExecutionEnv
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.stockOrders.StockOrderRepo
import com.merit.modules.products.ProductService
import com.merit.modules.stockOrders.StockOrderService
import utils.ExcelTestUtils._
import utils.ProductUtils._
import org.joda.time.DateTime
import com.merit.modules.products.ProductID
import scala.concurrent.duration._
import org.specs2.specification.BeforeEach
import scala.concurrent.Await
import org.scalamock.specs2.MockContext
import com.merit.modules.stockOrders.StockOrderID
import scala.concurrent.Future
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import com.merit.external.crawler.{CrawlerClient, SyncStockOrderMessage, SyncStockOrderResponse, SyncMessageProduct, SyncResponseProduct}
import com.merit.modules.excel.ExcelStockOrderRow
import com.merit.external.crawler.MessageType

class StockOrderServiceSpec(implicit ee: ExecutionEnv)
    extends ServiceSpec
    with FutureMatchers
    with BeforeEach {
  val timeout = 2.seconds
  override def before = {
    import schema._
    import schema.profile.api._

    val del = db.run(
      for {
        _ <- orderedProducts.delete
        _ <- stockOrders.delete
        _ <- products.delete
        _ <- categories.delete
        _ <- brands.delete
      } yield ()
    )

    Await.result(del, Duration.Inf)
  }

  "Stock order service" >> {
    "should increase qtys of existing products with 1" in new TestScope {
      val ps = sampleProducts
      val res = for {
        _ <- productService.batchInsertExcelRows(ps)
        _ <- stockOrderService.insertFromExcel(
          now,
          ps.map(excelProductRowToStockOrderRow(_).copy(qty = 1))
        )
        products <- productService.findAll(ps.map(_.barcode))
      } yield products
      res.map(_.sortBy(_.barcode).map(p => p.barcode -> p.qty)) must beEqualTo(
        ps.map(p => p.barcode -> (p.qty + 1))
      ).await(1, timeout)
      res.map(_.map(_.copy(id = ProductID.zero)).sortBy(_.barcode)) must beEqualTo(
        ps.map(p => excelRowToDTO(p).copy(qty = p.qty + 1))
      ).await(1, timeout)
    }

    "existing products should have twice the qty" in new TestScope {
      val ps = sampleProducts
      val res = for {
        _ <- productService.batchInsertExcelRows(ps)
        _ <- stockOrderService.insertFromExcel(
          now,
          ps.map(excelProductRowToStockOrderRow(_))
        )
        products <- productService.findAll(ps.map(_.barcode))
      } yield products
      res.map(_.map(_.copy(id = ProductID.zero)).sortBy(_.barcode)) must beEqualTo(
        ps.map(excelRowToDTO(_)).map(p => p.copy(qty = p.qty * 2))
      ).await(1, timeout)
    }

    "should create non existing products" in new TestScope {
      val ps = sampleProducts
      val res = for {
        _ <- stockOrderService.insertFromExcel(
          now,
          ps.map(excelProductRowToStockOrderRow(_))
        )
        products <- productService.findAll(ps.map(_.barcode))
      } yield products

      res.map(_.map(_.copy(id = ProductID.zero)).sortBy(_.barcode)) must beEqualTo(
        ps.map(excelRowToDTO(_))
      ).await(1, timeout)
    }

    "should handle both existing and nonexisting products" in new TestScope {
      val ps         = sampleProducts.map(_.copy(qty = 1))
      val firstHalf  = ps.take(5)
      val secondHalf = ps.drop(5)
      val res = for {
        _ <- productService.batchInsertExcelRows(firstHalf)
        _ <- stockOrderService.insertFromExcel(
          now,
          ps.map(excelProductRowToStockOrderRow(_))
        )
        products <- productService.getAll
      } yield products
      // existing products should have twice the qty
      res.map(_.map(_.copy(id = ProductID.zero)).sortBy(_.barcode)) must beEqualTo(
        (firstHalf.map(_.copy(qty = 2)) ++ secondHalf)
          .map(excelRowToDTO(_))
          .sortBy(_.barcode)
      ).await(1, timeout)
    }

    "should handle both existing and nonexisting products - 2" in new TestScope {
      val pToCreate       = sampleProducts
      val pToUpdate       = sampleProducts
      val orderedProducts = pToCreate ++ pToUpdate
      val res = for {
        _ <- productService.batchInsertExcelRows(pToUpdate)
        _ <- stockOrderService.insertFromExcel(
          now,
          orderedProducts.map(excelProductRowToStockOrderRow(_))
        )
        products <- productService.findAll(orderedProducts.map(_.barcode))
      } yield products
      res.map(_.map(_.copy(id = ProductID.zero)).sortBy(_.barcode)) must beEqualTo(
        (pToCreate ++ pToUpdate.map(p => p.copy(qty = p.qty * 2)))
          .sortBy(_.barcode)
          .map(excelRowToDTO(_))
      ).await(1, timeout)
    }

    "should return created and updated products separately" in new TestScope {
      val ps        = sampleProducts
      val pToUpdate = ps.take(5)
      val pToCreate = ps.drop(5)

      val res = for {
        _ <- productService.batchInsertExcelRows(pToUpdate)
        stockOrder <- stockOrderService.insertFromExcel(
          now,
          ps.map(excelProductRowToStockOrderRow(_))
        )
      } yield stockOrder
      res.map(_.date) must beEqualTo(now).await
      res.map(_.created.sortBy(_.barcode).map(p => (p.barcode, p.prevQty, p.ordered))) must beEqualTo(
        pToCreate.map(p => (p.barcode, 0, p.qty))
      ).await(1, timeout)
      res.map(
        _.updated.sortBy(_.barcode).map(p => (p.barcode, p.prevQty, p.ordered + p.prevQty))
      ) must beEqualTo(
        pToUpdate.map(p => (p.barcode, p.qty, p.qty * 2))
      ).await(1, timeout)
    }

    "sync stock order" in new TestScope {
      val res = for {
        products <- productService.batchInsertExcelRows(sampleProducts)
        s <- stockOrderService.insertFromExcel(
          now,
          products.map(
            p =>
              ExcelStockOrderRow(
                p.name,
                p.sku,
                p.variation,
                p.barcode,
                p.qty,
                p.price,
                p.discountPrice,
                None,
                None,
                None
              )
          )
        )
        _ <- stockOrderService.saveSyncResult(
          SyncStockOrderResponse(
            s.id,
            products.map(p => SyncResponseProduct(p.id, p.barcode, p.qty, true))
          )
        )
        stockOrder <- stockOrderService.get(s.id)
      } yield stockOrder

      res.map(_.map(_.products.map(_.synced).fold(true)(_ && _))) must beEqualTo(Some(true)).await
    }
  }

  class TestScope extends MockContext {
    val brandRepo      = BrandRepo(schema)
    val categoryRepo   = CategoryRepo(schema)
    val productRepo    = ProductRepo(schema)
    val stockOrderRepo = StockOrderRepo(schema)
    val crawlerClient  = mock[CrawlerClient]

    (crawlerClient.sendStockOrder _) expects (*) returning (Future(
      (SyncStockOrderMessage(StockOrderID(0), Seq()), SendMessageResponse.builder().build())
    ))

    val productService = ProductService(db, brandRepo, productRepo, categoryRepo)
    val stockOrderService =
      StockOrderService(
        db,
        stockOrderRepo,
        productRepo,
        brandRepo,
        categoryRepo,
        crawlerClient
      )

    def sampleProducts = getExcelProductRows(10).sortBy(_.barcode)
    val now            = DateTime.now()
  }
}
