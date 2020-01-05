package modules.stockOrders

import db.DbSpec
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.Scope
import com.merit.modules.products.ProductRepo
import com.merit.modules.stockOrders.StockOrderRepo
import org.specs2.concurrent.ExecutionEnv
import com.merit.modules.brands.BrandRepo
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.categories.CategoryRow
import utils.ProductUtils._
import com.merit.modules.stockOrders.StockOrderRow
import org.joda.time.DateTime
import com.merit.modules.stockOrders.StockOrderID
import com.merit.modules.products.OrderedProductRow
import com.merit.modules.products.ProductDTO
import slick.dbio.DBIO

class StockOrderRepoSpec(implicit ee: ExecutionEnv) extends DbSpec with FutureMatchers {
  "Stock order repo" >> {
    "should add a stock order" in new TestScope {
      val res = run(
        for {
          so <- stockOrderRepo.add(StockOrderRow(now))
        } yield so
      )
      res.map(_.date) must beEqualTo(now).await
      res.map(_.id).map { case StockOrderID(_) => 1 } must beEqualTo(1).await
    }

    "should add products to stock order with default quantities (1)" in new TestScope {
      val res = run(
        for {
          products <- insertTestData
          so       <- stockOrderRepo.add(StockOrderRow(now))
          _ <- stockOrderRepo.addProductsToStockOrder(
            products.map(p => OrderedProductRow(p.id, so.id))
          )
          stockOrder <- stockOrderRepo.get(so.id)
        } yield stockOrder
      )
      res.map(_.map(_._2.barcode)) must beEqualTo(sampleProducts.map(_.barcode)).await
      res.map(_.map(_._1.date).head) must beEqualTo(now).await
      res.map(_.map(_._3)) must beEqualTo((1 to 10).map(_ => 1)).await
    }

    "should add products to stock order with specified quantities" in new TestScope {
      val qtys = (1 to 10).map(_ => randomBetween(5))
      val res = run(
        for {
          products <- insertTestData
          so <- stockOrderRepo.add(StockOrderRow(now))
          _ <- stockOrderRepo.addProductsToStockOrder(
            products.zip(qtys).map(p => OrderedProductRow(p._1.id, so.id, p._2))
          )
          stockOrder <- stockOrderRepo.get(so.id)
        } yield stockOrder
      )
      res.map(_.map(_._2.barcode)) must beEqualTo(sampleProducts.map(_.barcode)).await
      res.map(_.map(_._3)) must beEqualTo(qtys).await
    }

    "should update synced property on ordered products" in new TestScope {
      val res = run(
        for {
          products <- insertTestData
          so       <- stockOrderRepo.add(StockOrderRow(now))
          _ <- stockOrderRepo.addProductsToStockOrder(
            products.map(p => OrderedProductRow(p.id, so.id))
          )
          _ <- DBIO.sequence(products.map(p => stockOrderRepo.syncOrderedProduct(so.id, p.id, true)))
          stockOrder <- stockOrderRepo.get(so.id)
        } yield stockOrder
      )
      res.map(_.map(_._4).fold(true)(_ && _)) must beTrue.await
    }
  }

  class TestScope extends Scope {
    val brandRepo      = BrandRepo(schema)
    val categoryRepo   = CategoryRepo(schema)
    val productRepo    = ProductRepo(schema)
    val stockOrderRepo = StockOrderRepo(schema)

    val sampleProducts = (1 to 10).map(_ => createProduct).sortBy(_.barcode)
    val now            = DateTime.now()

    val insertTestData = for {
      brand    <- brandRepo.insert(BrandRow("b1"))
      category <- categoryRepo.insert(CategoryRow("c1"))
      products <- productRepo.batchInsert(sampleProducts)
    } yield products
  }
}
