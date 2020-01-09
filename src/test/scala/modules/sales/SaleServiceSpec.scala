package modules.sales

import org.specs2.concurrent.ExecutionEnv
import db.DbSpecification
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.Scope
import com.merit.modules.brands.{BrandRepo, BrandRow}
import com.merit.modules.categories.{CategoryRepo, CategoryRow}
import com.merit.modules.products.{ProductRepo, ProductService, ProductID}
import com.merit.modules.sales.{SaleRepo, SaleService, SaleSummaryProduct}
import utils.ExcelTestUtils._
import utils.ProductUtils._
import com.merit.modules.excel.{ExcelProductRow, ExcelSaleRow}
import org.joda.time.DateTime
import scala.util.Random
import cats.implicits._
import com.merit.external.crawler.{SyncSaleResponse, SyncResponseProduct}
import org.scalamock.specs2.MockContext
import com.merit.external.crawler.CrawlerClient
import scala.concurrent.Future
import com.merit.external.crawler.SyncSaleMessage
import com.merit.modules.sales.SaleID
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import com.merit.external.crawler.MessageType
import com.merit.modules.products.Currency
import org.specs2.specification.AfterEach
import scala.concurrent.Await
import scala.concurrent.duration._

class SaleServiceSpec(implicit ee: ExecutionEnv)
    extends DbSpecification
    with FutureMatchers
    with AfterEach {
      val timeout = 2.seconds
  override def after = {
    import schema._
    import schema.profile.api._

    val del = db.run(
      for {
        _ <- soldProducts.delete
        _ <- sales.delete
        _ <- products.delete
        _ <- categories.delete
        _ <- brands.delete
      } yield ()
    )

    Await.result(del, Duration.Inf)
  }
  "Sale service" >> {
    "should insert excel rows with default qty" in new TestScope {
      val sale = for {
        products <- productService.batchInsertExcelRows(sampleProducts)
        sale <- saleService.insertFromExcel(
          sampleProducts.map(p => ExcelSaleRow(p.barcode, 1)),
          now,
          total
        )
      } yield sale
      sale.map(_.products.map(p => (p.barcode, p.prevQty, p.soldQty)).sortBy(_._1)) must beEqualTo(
        sampleProducts.map(p => (p.barcode, p.qty, 1)).sortBy(_._1)
      ).await(1, timeout)
    }

    "should insert excel rows with specified quantities" in new TestScope {
      val qtys = (1 to 5).toSeq
      val sale = for {
        products <- productService.batchInsertExcelRows(sampleProducts)
        sale <- saleService.insertFromExcel(
          sampleProducts.zip(qtys).map(p => ExcelSaleRow(p._1.barcode, p._2)),
          now,
          total
        )
      } yield sale
      sale.map(_.products.map(p => (p.barcode, p.prevQty, p.soldQty)).sortBy(_._1)) must beEqualTo(
        sampleProducts
          .zip(qtys)
          .map(p => (p._1.barcode, p._1.qty, p._2))
          .sortBy(_._1)
      ).await(1, timeout)
    }

    "should insert excel rows with specified quantities - 2" in new TestScope {
      val qtys = (1 to 5).map(_ => Random.nextInt(20))
      val sale = for {
        products <- productService.batchInsertExcelRows(sampleProducts)
        sale <- saleService.insertFromExcel(
          sampleProducts.zip(qtys).map(p => ExcelSaleRow(p._1.barcode, p._2)),
          now,
          total
        )
      } yield sale
      sale.map(_.products.map(p => (p.barcode, p.prevQty, p.soldQty)).sortBy(_._1)) must beEqualTo(
        sampleProducts
          .zip(qtys)
          .map(p => (p._1.barcode, p._1.qty, p._2))
          .sortBy(_._1)
      ).await(1, timeout)
    }

    "should do nothing when barcodes are not found" in new TestScope {
      val nonExistingProducts = getExcelProductRows(5)
      val sale = for {
        sale <- saleService.insertFromExcel(
          nonExistingProducts.map(p => ExcelSaleRow(p.barcode, 1)),
          now,
          total
        )
      } yield sale
      sale.map(_.products.length) must beEqualTo(0).await
    }

    "should get a sale with id" in new TestScope {
      val products = getExcelProductRows(5).sortBy(_.barcode)
      val sale = for {
        products <- productService.batchInsertExcelRows(products)
        s <- saleService.insertFromExcel(
          products.map(p => ExcelSaleRow(p.barcode, 1)),
          now,
          total
        )
        sale <- saleService.getSale(s.id)
      } yield sale
      sale.map(_.map(_.createdAt)) must beEqualTo(Some(now)).await
      sale.map(_.map(_.products.map(_.copy(id = ProductID.zero)))) must beEqualTo(
        Some(
          products.map(excelRowToSaleDTOProduct(_).copy(qty = 1))
        )
      ).await(1, timeout)
    }

    "should get a sale with id - 2" in new TestScope {
      val products = getExcelProductRows(5).sortBy(_.barcode)
      val qtys     = (1 to 5).map(_ => randomBetween(5))
      val sale = for {
        products <- productService.batchInsertExcelRows(products)
        s <- saleService.insertFromExcel(
          products.zip(qtys).map(p => ExcelSaleRow(p._1.barcode, p._2)),
          now,
          total
        )
        sale <- saleService.getSale(s.id)
      } yield sale
      sale.map(
        _.map(_.products.map(_.copy(id = ProductID.zero))) must beEqualTo(
          Some(
            products.zip(qtys).map(p => excelRowToSaleDTOProduct(p._1).copy(qty = p._2))
          )
        )
      ).await(1, timeout)
    }

    "should sync sale" in new TestScope {
      val products = getExcelProductRows(5).sortBy(_.barcode)
      val sale = for {
        products <- productService.batchInsertExcelRows(products)
        s <- saleService.insertFromExcel(
          products.map(p => ExcelSaleRow(p.barcode)),
          now,
          total
        )
        _ <- saleService.saveSyncResult(
          SyncSaleResponse(
            s.id,
            products.map(p => SyncResponseProduct(p.id, p.barcode, p.qty, true))
          )
        )
        sale <- saleService.getSale(s.id)
      } yield sale
      sale.map(_.map(_.products.map(_.synced).fold(true)(_ && _))) must beEqualTo(Some(true)).await
    }
  }

  class TestScope extends MockContext {
    val brandRepo     = BrandRepo(schema)
    val categoryRepo  = CategoryRepo(schema)
    val productRepo   = ProductRepo(schema)
    val saleRepo      = SaleRepo(schema)
    val crawlerClient = mock[CrawlerClient]
    val total         = Currency(1000)

    (crawlerClient.sendSale _) expects (*) returning (Future(
      (SyncSaleMessage(SaleID(0), Seq()), SendMessageResponse.builder().build())
    ))

    val productService = ProductService(db, brandRepo, productRepo, categoryRepo)
    val saleService    = SaleService(db, saleRepo, productRepo, crawlerClient)
    val sampleProducts = getExcelProductRows(5).sortBy(_.barcode)
    val now            = DateTime.now()
  }
}
