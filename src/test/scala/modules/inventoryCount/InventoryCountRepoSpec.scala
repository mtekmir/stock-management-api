package modules.inventoryCount

import org.specs2.specification.Scope
import com.merit.modules.brands.BrandRepo
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.products.ProductRepo
import com.merit.modules.inventoryCount.InventoryCountRepo
import com.merit.modules.inventoryCount.InventoryCountBatchRow
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.BeforeEach
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import db.DbSpecification
import com.merit.modules.inventoryCount.InventoryCountBatchID
import utils.ExcelTestUtils._
import utils.ProductUtils._
import org.joda.time.DateTime
import com.merit.modules.inventoryCount.InventoryCountStatus._
import com.merit.modules.inventoryCount.InventoryCountProductStatus._
import com.merit.modules.inventoryCount.InventoryCountProductRow
import com.merit.modules.products.ProductService
import slick.dbio.DBIO

class InventoryCountRepoSpec(implicit ee: ExecutionEnv)
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

  "Inventory Count Repo" >> {
    "should insert a batch and get it" in new TestScope {
      (db
        .run(for {
          row   <- invCountRepo.insertBatch(InventoryCountBatchRow(now, None, None, None, None))
          dto   <- invCountRepo.get(row.id)
          count <- invCountRepo.count(Open)
        } yield {
          dto.map(_._1) must beSome(row)
          count must beEqualTo(1)
        }))
        .await
    }

    "should insert products to batch and get them" in new TestScope {
      val ps = sampleProducts(10)
      (for {
        row <- db.run(
          invCountRepo.insertBatch(InventoryCountBatchRow(now, None, None, None, None))
        )
        products <- productService.batchInsertExcelRows(ps)
        _ <- db.run(
          invCountRepo.addProductsToBatch(
            products.map(p => InventoryCountProductRow(row.id, p.id, p.qty, now))
          )
        )
        batchProducts <- db.run(invCountRepo.getBatchProducts(row.id, All, 1, 10))
      } yield {
        batchProducts.map(p => (p.barcode, p.expected, p.counted)).sortBy(_._1) must beEqualTo(
          ps.map(p => (p.barcode, p.qty, None))
        )
      }).await
    }

    "should count products and get them" in new TestScope {
      val ps   = sampleProducts(10)
      val qtys = (1 to 10).map(_ => randomBetween(5))
      (for {
        row <- db.run(
          invCountRepo.insertBatch(InventoryCountBatchRow(now, None, None, None, None))
        )
        products <- productService.batchInsertExcelRows(ps)
        _ <- db.run(
          invCountRepo.addProductsToBatch(
            products.map(p => InventoryCountProductRow(row.id, p.id, p.qty, now))
          )
        )
        batchProducts <- db.run(invCountRepo.getBatchProducts(row.id, All, 1, 10))

        _ <- db.run(DBIO.sequence(batchProducts.sortBy(_.barcode).zip(qtys).map {
          case (p, qty) => invCountRepo.countProduct(p.id, qty)
        }))

        updatedBatchProducts <- db.run(invCountRepo.getBatchProducts(row.id, Counted, 1, 10))
      } yield {
        updatedBatchProducts.length must beEqualTo(10)
        updatedBatchProducts
          .sortBy(_.barcode)
          .map(p => (p.barcode, p.expected)) must beEqualTo(
          ps.sortBy(_.barcode).map(p => (p.barcode, p.qty))
        )
        updatedBatchProducts.sortBy(_.barcode).map(p => (p.barcode, p.counted)) must beEqualTo(
          ps.sortBy(_.barcode).zip(qtys).map { case (p, qty) => (p.barcode, Some(qty)) }
        )
      }).await
    }

    "should change status of inventory count and delete it" in new TestScope {
      val ps = sampleProducts(1)
      (db
        .run(for {
          row      <- invCountRepo.insertBatch(InventoryCountBatchRow(now, None, None, None, None))
          _        <- invCountRepo.cancelInventoryCount(row.id)
          updated1 <- invCountRepo.get(row.id)
          _        <- invCountRepo.completeInventoryCount(row.id)
          updated2 <- invCountRepo.get(row.id)
          _        <- invCountRepo.deleteBatch(row.id)
          updated3 <- invCountRepo.get(row.id)
        } yield {
          updated1.map(_._1.status) must beEqualTo(Some(Cancelled))
          updated2.map(_._1.status) must beEqualTo(Some(Completed))
          updated3.map(_._1) must beNone
        }))
        .await
    }

    "should delete inventory count products" in new TestScope {
      val ps = sampleProducts(2)
      (for {
        row <- db.run(
          invCountRepo.insertBatch(InventoryCountBatchRow(now, None, None, None, None))
        )
        products <- productService.batchInsertExcelRows(ps)
        _ <- db.run(
          invCountRepo.addProductsToBatch(
            products.map(p => InventoryCountProductRow(row.id, p.id, p.qty, now))
          )
        )
        _             <- db.run(invCountRepo.deleteAllInventoryCountProducts(row.id))
        batchProducts <- db.run(invCountRepo.getBatchProducts(row.id, All, 1, 10))
      } yield {
        batchProducts.length must beEqualTo(0)
      }).await
    }
  }

  class TestScope extends Scope {
    val brandRepo      = BrandRepo(schema)
    val categoryRepo   = CategoryRepo(schema)
    val productRepo    = ProductRepo(schema)
    val invCountRepo   = InventoryCountRepo(schema)
    val productService = ProductService(db, brandRepo, productRepo, categoryRepo)
    val testBatchId    = InventoryCountBatchID(0L)
    def sampleProducts(q: Int = 10) =
      getExcelProductRows(q, Some("Babolat"), Some("Racket")).sortBy(_.barcode)
    val now = DateTime.now()
  }
}
