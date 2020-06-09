package modules.inventoryCount

import org.specs2.concurrent.ExecutionEnv
import db.DbSpecification
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.BeforeEach
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.merit.modules.brands.BrandRepo
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.products.ProductRepo
import com.merit.modules.stockOrders.StockOrderRepo
import com.merit.external.crawler.CrawlerClient
import scala.concurrent.Future
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import com.merit.modules.stockOrders.StockOrderID
import com.merit.external.crawler.SyncStockOrderMessage
import com.merit.modules.products.ProductService
import com.merit.modules.inventoryCount.InventoryCountService
import com.merit.modules.inventoryCount.InventoryCountRepo
import utils.ExcelTestUtils._
import utils.ProductUtils._
import org.joda.time.DateTime
import com.merit.modules.inventoryCount.InventoryCountStatus
import com.merit.modules.inventoryCount.InventoryCountProductStatus
import com.merit.external.crawler.SyncInventoryCountMessage
import com.merit.modules.inventoryCount.InventoryCountBatchID
import com.merit.modules.inventoryCount.InventoryCountDTO
import com.merit.modules.excel.ExcelProductRow
import slick.dbio.DBIO
import com.merit.modules.inventoryCount.InventoryCountProductID
import com.merit.modules.products.ProductDTO
import com.merit.modules.inventoryCount.InventoryCountProductDTO
import com.merit.external.crawler.SyncSaleMessage
import com.merit.modules.sales.SaleSummary
import com.merit.modules.stockOrders.StockOrderSummary
import org.specs2.specification.Scope

class InventoryCountServiceSpec(implicit ee: ExecutionEnv)
    extends DbSpecification
    with FutureMatchers
    with BeforeEach {
  override def before = {
    import schema._
    import schema.profile.api._

    val del = db.run(
      for {
        _ <- inventoryCountProducts.delete
        _ <- inventoryCountBatches.delete
        _ <- products.delete
      } yield ()
    )

    Await.result(del, Duration.Inf)
  }

  "InventoryCountService" >> {
    import InventoryCountProductStatus._
    import InventoryCountStatus._
    "should create a batch + return batch and products" in new TestScope {
      val ps = sampleProducts()
      (for {
        dto           <- insertBatchWithProducts(ps)
        batchProducts <- invCountService.getBatchProducts(dto.id, All, 1, 20)
        getDto        <- invCountService.getBatch(dto.id)
      } yield {
        // dtos fields should be correct
        dto.copy(id = testBatchId) must beEqualTo(
          InventoryCountDTO(
            testBatchId,
            Open,
            now,
            None,
            Some("test"),
            Some("Racket"),
            Some("Babolat")
          )
        )
        // expected counts of the products should be the same as sample products
        batchProducts.products
          .map(p => (p.barcode, p.expected, p.counted))
          .sortBy(_._1) must beEqualTo(
          ps.map(p => (p.barcode, p.qty, None)).sortBy(_._1)
        )
        // service should be able to get the batch correctly
        getDto must beSome(dto)
      }).await
    }

    "should be able to count products" in new TestScope {
      val ps = sampleProducts()
      (for {
        dto           <- insertBatchWithProducts(ps.slice(0, 2))
        batchProducts <- invCountService.getBatchProducts(dto.id, All, 1, 20)
        p1 = batchProducts.products.head
        p2 = batchProducts.products(1)
        _                    <- invCountService.countProduct(p1.id, 10)
        _                    <- invCountService.countProduct(p2.id, 20)
        countedProductsRes   <- invCountService.getBatchProducts(dto.id, Counted, 1, 20)
        unCountedProductsRes <- invCountService.getBatchProducts(dto.id, UnCounted, 1, 20)
      } yield {
        countedProductsRes.products
          .map(p => (p.barcode, p.expected, p.counted))
          .sortBy(_._1) must beEqualTo(
          Seq((p1.barcode, p1.expected, Some(10)), (p2.barcode, p2.expected, Some(20)))
            .sortBy(_._1)
        )
        unCountedProductsRes.products must beEqualTo(Seq())
        unCountedProductsRes.counted must beEqualTo(2)
        unCountedProductsRes.uncounted must beEqualTo(0)
      }).await
    }

    "should complete inventory count 1" in new TestScope {
      val ps = sampleProducts()
      (for {
        batch            <- insertBatchWithProducts(ps)
        firstCompleteRes <- invCountService.complete(batch.id)
        batchProducts    <- invCountService.getBatchProducts(batch.id, All, 1, 20)
        p1 = batchProducts.products.head
        p2 = batchProducts.products(1)
        _                 <- invCountService.countProduct(p1.id, 10)
        _                 <- invCountService.countProduct(p2.id, 11)
        secondCompleteRes <- invCountService.complete(batch.id)
        thirdCompleteRes  <- invCountService.complete(batch.id, force = true)
        updatedProducts <- db.run(
          DBIO.sequence(Seq(p1, p2).map(p => productRepo.get(p.barcode)))
        )
      } yield {
        firstCompleteRes must beEqualTo(Left("None of the products are counted."))
        secondCompleteRes must beEqualTo(Left("Most of the products are not counted."))
        thirdCompleteRes.map(_.status) must beEqualTo(Right(Completed))
        updatedProducts.map(_.map(p => (p.barcode, p.qty))) must beEqualTo(
          Seq(Some((p1.barcode, 10)), Some((p2.barcode, 11)))
        )
        checkQInRegistry2(updatedProducts)
      }).await
    }

    "should complete inventory count 2" in new TestScope {
      val ps = sampleProducts()
      (for {
        batch         <- insertBatchWithProducts(ps)
        batchProducts <- invCountService.getBatchProducts(batch.id, All, 1, 20)
        _ <- Future.sequence(
          batchProducts.products
            .map(p => (p.id, ps.find(_.barcode == p.barcode).map(_.qty)))
            .map {
              case (id, qty) => invCountService.countProduct(id, qty.get + 5)
            }
        )
        updatedBatchProducts <- invCountService.getBatchProducts(batch.id, All, 1, 20)
        _                    <- invCountService.complete(batch.id)
        updatedProducts <- db.run(
          DBIO.sequence(ps.map(p => productRepo.get(p.barcode)))
        )
      } yield {
        checkQuantities(updatedProducts, ps.map(p => p.copy(qty = p.qty + 5)))
        updatedBatchProducts.products
          .map(p => (p.barcode, p.expected, p.counted))
          .sortBy(_._1) must beEqualTo(
          ps.map(p => (p.barcode, p.qty, Some(p.qty + 5))).sortBy(_._1)
        )
        checkQInRegistry(updatedBatchProducts.products)
        checkQInRegistry2(updatedProducts)
      }).await
    }

    "should insert from excel 1" in new TestScope {
      val ps   = sampleProducts()
      val rows = ps.map(excelProductRowToStockOrderRow(_))
      (for {
        p <- productService.batchInsertExcelRows(
          ps.slice(0, 5).map(p => p.copy(qty = p.qty - 3))
        )
        dto <- invCountService.insertFromExcel(now, rows)
        updatedProducts <- db.run(
          DBIO.sequence(ps.map(p => productRepo.get(p.barcode)))
        )
        updatedBatchProducts <- invCountService.getBatchProducts(dto.id, All, 1, 20)
      } yield {
        checkQuantities(updatedProducts, ps)
        updatedBatchProducts.products
          .map(p => (p.barcode, p.counted))
          .sortBy(_._1) must beEqualTo(
          ps.map(p => (p.barcode, Some(p.qty))).sortBy(_._1)
        )
        checkQInRegistry(updatedBatchProducts.products)
        checkQInRegistry2(updatedProducts)
      }).await
    }

    "should insert from excel by inserting new products" in new TestScope {
      val ps   = sampleProducts()
      val rows = ps.map(excelProductRowToStockOrderRow(_))
      (for {
        dto                  <- invCountService.insertFromExcel(now, rows)
        updatedProducts      <- db.run(DBIO.sequence(ps.map(p => productRepo.get(p.barcode))))
        updatedBatchProducts <- invCountService.getBatchProducts(dto.id, All, 1, 20)
      } yield {
        dto.copy(id = testBatchId) must beEqualTo(
          InventoryCountDTO(
            testBatchId,
            Completed,
            now,
            Some(now),
            Some(s"Imported from excel at ${now.toString("dd-MM-yyyy HH:mm")}"),
            None,
            None
          )
        )
        checkQuantities(updatedProducts, ps)
        updatedBatchProducts.products
          .map(p => (p.barcode, p.counted))
          .sortBy(_._1) must beEqualTo(
          ps.map(p => (p.barcode, Some(p.qty))).sortBy(_._1)
        )
        checkQInRegistry(updatedBatchProducts.products)
        checkQInRegistry2(updatedProducts)
      }).await
    }

    "should insert from excel by updating quantities of existing products" in new TestScope {
      val ps   = sampleProducts()
      val rows = ps.map(excelProductRowToStockOrderRow(_))
      (for {
        _                    <- productService.batchInsertExcelRows(ps)
        dto                  <- invCountService.insertFromExcel(now, rows.map(r => r.copy(qty = r.qty + 5)))
        updatedProducts      <- db.run(DBIO.sequence(ps.map(p => productRepo.get(p.barcode))))
        updatedBatchProducts <- invCountService.getBatchProducts(dto.id, All, 1, 20)
      } yield {
        checkQInRegistry(updatedBatchProducts.products)
        checkQInRegistry2(updatedProducts)
        checkQuantities(updatedProducts, ps.map(p => p.copy(qty = p.qty + 5)))
      }).await
    }

    "should search products in batch" in new TestScope {
      val ps = sampleProducts(5)
      val p1 = ps(0)
      val p2 = ps(1)
      (for {
        dto   <- insertBatchWithProducts(ps)
        p1Res <- invCountService.searchBatchProducts(dto.id, p1.barcode)
        p2Res <- invCountService.searchBatchProducts(dto.id, p2.barcode)
      } yield {
        p1Res.map(p => (p.barcode, p.expected, p.counted)) must beEqualTo(
          Seq((p1.barcode, p1.qty, None))
        )
        p2Res.map(p => (p.barcode, p.expected, p.counted)) must beEqualTo(
          Seq((p2.barcode, p2.qty, None))
        )
      }).await
    }

    "should delete product and batch" in new TestScope {
      val ps = sampleProducts(2)
      (for {
        dto             <- insertBatchWithProducts(ps)
        batchProductRes <- invCountService.getBatchProducts(dto.id, All, 1, 10)
        p1 = batchProductRes.products(0)
        p2 = batchProductRes.products(1)
        _      <- invCountService.deleteProduct(p1.id)
        res1   <- invCountService.getBatchProducts(dto.id, All, 1, 10)
        _      <- invCountService.deleteProduct(p2.id)
        res2   <- invCountService.getBatchProducts(dto.id, All, 1, 10)
        _      <- invCountService.delete(dto.id)
        delRes <- invCountService.getBatch(dto.id)
      } yield {
        res1.products.length must beEqualTo(1)
        res2.products.length must beEqualTo(0)
        delRes must beNone
      }).await
    }

    "should correctly update quantities" in new TestScope {
      val ps   = sampleProducts(20)
      val qtys = (1 to 20).map(_ => randomBetween(10)).toSeq
      (for {
        dto             <- insertBatchWithProducts(ps)
        batchProductRes <- invCountService.getBatchProducts(dto.id, All, 1, 20)
        _ <- Future.sequence(batchProductRes.products.sortBy(_.barcode).zip(qtys).map {
          case (p, qty) => invCountService.countProduct(p.id, qty)
        })
        _ <- invCountService.complete(dto.id)
        updatedProducts <- db.run(
          DBIO.sequence(ps.map(p => productRepo.get(p.barcode)))
        )
        updatedBatchProducts <- invCountService.getBatchProducts(dto.id, All, 1, 20)
      } yield {
        checkQuantities(updatedProducts, ps.sortBy(_.barcode).zip(qtys).map {
          case (p, q) => p.copy(qty = q)
        })
        checkQInRegistry(updatedBatchProducts.products)
        checkQInRegistry2(updatedProducts)
      }).await
    }
  }

  class TestScope extends Scope {
    val brandRepo       = BrandRepo(schema)
    val categoryRepo    = CategoryRepo(schema)
    val productRepo     = ProductRepo(schema)
    val invCountRepo    = InventoryCountRepo(schema)
    var crawlerRegistry = Map[String, Int]()
    val crawlerClient = new CrawlerClient {
      def sendSale(sale: SaleSummary)                   = ???
      def sendStockOrder(stockOrder: StockOrderSummary) = ???
      def sendInventoryCount(b: InventoryCountDTO, products: Seq[InventoryCountProductDTO]) = {
        crawlerRegistry = products.foldLeft(Map[String, Int]()) {
          case (m, p) => m + (p.barcode -> p.counted.getOrElse(0))
        }
        Future.successful(Seq(), Seq())
      }
    }

    val productService = ProductService(db, brandRepo, productRepo, categoryRepo)
    val invCountService = InventoryCountService(
      db,
      invCountRepo,
      productRepo,
      brandRepo,
      categoryRepo,
      crawlerClient
    )

    val testBatchId = InventoryCountBatchID(0L)
    def sampleProducts(q: Int = 10) =
      getExcelProductRows(q, Some("Babolat"), Some("Racket")).sortBy(_.barcode)

    def insertBatchWithProducts(
      ps: Seq[ExcelProductRow],
      brand: String = "Babolat",
      category: String = "Racket",
      name: String = "test"
    ) =
      for {
        p   <- productService.batchInsertExcelRows(ps)
        dto <- invCountService.create(Some(now), Some(name), p.head.brandId, p.head.categoryId)
      } yield (dto)
    val now = DateTime.now()

    def checkQuantities(updated: Seq[Option[ProductDTO]], ps: Seq[ExcelProductRow]) =
      updated.flatten.map(p => (p.barcode, p.qty)).sortBy(_._1) must beEqualTo(
        ps.map(p => (p.barcode, p.qty)).sortBy(_._1)
      )
    def checkQInRegistry(ps: Seq[InventoryCountProductDTO]) =
      ps.map(p => {
        crawlerRegistry.get(p.barcode) must beSome(p.counted.getOrElse(0))
      })
    def checkQInRegistry2(ps: Seq[Option[ProductDTO]]) = ps.map {
      case Some(p) => crawlerRegistry.get(p.barcode) must beSome(p.qty)
      case None    => 2 must beEqualTo(4)
    }
  }
}
