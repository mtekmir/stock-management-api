package modules.sales

import org.specs2.concurrent.ExecutionEnv
import db.DbSpec
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.Scope
import com.merit.modules.products.ProductRepo
import com.merit.modules.sales.SaleRepo
import com.merit.modules.brands.BrandRepo
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import utils.ProductUtils._
import com.merit.modules.sales.SaleRow
import org.joda.time.DateTime
import com.merit.modules.sales.SaleID
import com.merit.modules.products.SoldProductRow

class SaleRepoSpec(implicit ee: ExecutionEnv) extends DbSpec with FutureMatchers {

  "Sale Repo" >> {
    "should create a sale" in new TestScope {
      val now = DateTime.now()
      val res = run(
        for {
          saleRow <- saleRepo.add(SaleRow(now))
        } yield saleRow
      )
      res.map{ case SaleRow(now, _) => now } must beEqualTo(now).await
    }

    "should add products to sale with default qty(1)" in new TestScope {
      val res = run(
        for {
          products <- insertTestData
          saleRow <- saleRepo.add(SaleRow())
          _ <- saleRepo.addProductsToSale(products.map(p => SoldProductRow(p.id, saleRow.id)))
          sale <- saleRepo.get(saleRow.id)
        } yield sale
      )
      // products
      res.map(_.map(_._2.barcode).sorted) must beEqualTo(sampleProducts.map(_.barcode)).await
      // Qtys
      res.map(_.map(_._3)) must beEqualTo((1 to 5).map(_ => 1)).await
    }

    "should add products to sale with specified qty" in new TestScope {
      val res = run(
        for {
          products <- insertTestData
          saleRow <- saleRepo.add(SaleRow())
          _ <- saleRepo.addProductsToSale(products.zipWithIndex.map(p => SoldProductRow(p._1.id, saleRow.id, p._2 + 1)))
          sale <- saleRepo.get(saleRow.id)
        } yield sale
      )

      // products
      res.map(_.map(_._2.barcode).sorted) must beEqualTo(sampleProducts.map(_.barcode)).await
      // Qtys
      res.map(_.map(_._3)) must beEqualTo((1 to 5).map(i => i)).await
    }
  }

  class TestScope extends Scope {
    val brandRepo      = BrandRepo(schema)
    val categoryRepo   = CategoryRepo(schema)
    val productRepo    = ProductRepo(schema)
    val saleRepo       = SaleRepo(schema)
    val sampleProducts = (1 to 5).map(i => createProduct).sortBy(_.barcode)

    def insertTestData =
      for {
        brandRow    <- brandRepo.insert(BrandRow("brand1"))
        categoryRow <- categoryRepo.insert(CategoryRow("category1"))
        products <- productRepo.batchInsert(
          sampleProducts
            .map(_.copy(brandId = Some(brandRow.id), categoryId = Some(categoryRow.id)))
        )
      } yield products
  }
}
