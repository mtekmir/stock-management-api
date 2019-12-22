package modules.sales

import org.specs2.concurrent.ExecutionEnv
import db.ServiceSpec
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

class SaleServiceSpec(implicit ee: ExecutionEnv) extends ServiceSpec with FutureMatchers {
  "Sale service" >> {
    "should insert excel rows with default qty" in new TestScope {
      val sale = for {
        products <- productService.batchInsertExcelRows(sampleProducts)
        sale <- saleService.insertFromExcel(
          sampleProducts.map(p => ExcelSaleRow(p.barcode, 1)),
          now
        )
      } yield sale
      sale.map(_.products.map(p => (p.barcode, p.prevQty, p.soldQty)).sortBy(_._1)) must beEqualTo(
        sampleProducts.map(p => (p.barcode, p.qty, 1)).sortBy(_._1)
      ).await
    }

    "should insert excel rows with specified quantities" in new TestScope {
      val qtys = (1 to 5).toSeq
      val sale = for {
        products <- productService.batchInsertExcelRows(sampleProducts)
        sale <- saleService.insertFromExcel(
          sampleProducts.zip(qtys).map(p => ExcelSaleRow(p._1.barcode, p._2)),
          now
        )
      } yield sale
      sale.map(_.products.map(p => (p.barcode, p.prevQty, p.soldQty)).sortBy(_._1)) must beEqualTo(
        sampleProducts
          .zip(qtys)
          .map(p => (p._1.barcode, p._1.qty, p._2))
          .sortBy(_._1)
      ).await
    }

    "should insert excel rows with specified quantities - 2" in new TestScope {
      val qtys = (1 to 5).map(_ => Random.nextInt(20))
      val sale = for {
        products <- productService.batchInsertExcelRows(sampleProducts)
        sale <- saleService.insertFromExcel(
          sampleProducts.zip(qtys).map(p => ExcelSaleRow(p._1.barcode, p._2)),
          now
        )
      } yield sale
      sale.map(_.products.map(p => (p.barcode, p.prevQty, p.soldQty)).sortBy(_._1)) must beEqualTo(
        sampleProducts
          .zip(qtys)
          .map(p => (p._1.barcode, p._1.qty, p._2))
          .sortBy(_._1)
      ).await
    }

    "should do nothing when barcodes are not found" in new TestScope {
      val nonExistingProducts = getExcelProductRows(5)
      val sale = for {
        sale <- saleService.insertFromExcel(
          nonExistingProducts.map(p => ExcelSaleRow(p.barcode, 1)),
          now
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
          now
        )
        sale <- saleService.getSale(s.id)
      } yield sale
      sale.map(_.map(_.createdAt)) must beEqualTo(Some(now)).await
      sale.map(_.map(_.products.map(_.copy(id = ProductID.zero)))) must beEqualTo(
        Some(
          products.map(excelRowToSaleDTOProduct(_).copy(qty = 1))
        )
      ).await
    }

    "should get a sale with id - 2" in new TestScope {
      val products = getExcelProductRows(5).sortBy(_.barcode)
      val qtys     = (1 to 5).map(_ => randomBetween(5))
      val sale = for {
        products <- productService.batchInsertExcelRows(products)
        s <- saleService.insertFromExcel(
          products.zip(qtys).map(p => ExcelSaleRow(p._1.barcode, p._2)),
          now
        )
        sale <- saleService.getSale(s.id)
      } yield sale
      sale.map(
        _.map(_.products.map(_.copy(id = ProductID.zero))) must beEqualTo(
          Some(
            products.zip(qtys).map(p => excelRowToSaleDTOProduct(p._1).copy(qty = p._2))
          )
        )
      )
    }

    "should sync sale" in new TestScope {
      val products = getExcelProductRows(5).sortBy(_.barcode)
      val sale = for {
        products <- productService.batchInsertExcelRows(products)
        s <- saleService.insertFromExcel(
          products.map(p => ExcelSaleRow(p.barcode)),
          now
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

  class TestScope extends Scope {
    val brandRepo    = BrandRepo(schema)
    val categoryRepo = CategoryRepo(schema)
    val productRepo  = ProductRepo(schema)
    val saleRepo     = SaleRepo(schema)

    val productService = ProductService(db, brandRepo, productRepo, categoryRepo)
    val saleService    = SaleService(db, saleRepo, productRepo)
    val sampleProducts = getExcelProductRows(5).sortBy(_.barcode)
    val now            = DateTime.now()
  }
}
