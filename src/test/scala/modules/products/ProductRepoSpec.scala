package modules.products

import org.specs2.matcher.FutureMatchers
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.Scope
import com.merit.modules.products.ProductRepo
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import utils.ProductUtils._
import com.merit.modules.products.ProductDTO
import com.merit.modules.products.ProductID
import db.DbSpecification
import org.specs2.specification.AfterEach
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.merit.modules.products.ProductFilters

class ProductRepoSpec(implicit ee: ExecutionEnv)
    extends DbSpecification
    with FutureMatchers
    with AfterEach {
  override def after: Any = {
    import schema._
    import schema.profile.api._

    val del = db.run(
      for {
        _ <- products.delete
        _ <- categories.delete
        _ <- brands.delete
      } yield ()
    )
    Await.result(del, Duration.Inf)
  }

  "Product Repo" >> {
    "should insert a product" in new TestScope {
      val p = createProduct
      val res = db.run(
        for {
          (brand, category, _) <- insertTestData
          productId <- productRepo.insert(
            p.copy(brandId = Some(brand.id), categoryId = Some(category.id))
          )
          product <- productRepo.get(p.barcode)
        } yield product.map(_.copy(id = ProductID.zero))
      )
      res.map(_.map(_.brand.map(_.name).getOrElse(""))) must beEqualTo(Some("b1")).await
      res.map(_.map(_.category.map(_.name).getOrElse(""))) must beEqualTo(Some("c1")).await
      res.map(_.map(_.barcode)) must beEqualTo(Some(p.barcode)).await
    }

    "should get a product by barcode" in new TestScope {
      val p = createProduct
      val res = db.run(
        for {
          _       <- productRepo.insert(p)
          product <- productRepo.get(p.barcode)
        } yield product.map(_.copy(id = ProductID.zero))
      )
      res must beEqualTo(Some(rowToDTO(p))).await
    }

    "should get multiple products by barcode" in new TestScope {
      val ps = Seq(createProduct, createProduct).sortBy(_.barcode)
      val res = db.run(
        for {
          _        <- productRepo.batchInsert(ps)
          products <- productRepo.findAll(ps.map(_.barcode))
        } yield products.map(_.copy(id = ProductID.zero))
      )
      res.map(_.sortBy(_.barcode)) must beEqualTo(ps.map(p => rowToDTO(p))).await
    }

    "should get multiple products with Category and Brand by barcode" in new TestScope {
      val res = db.run(
        for {
          (_, _, p) <- insertTestData
          products  <- productRepo.findAll(p.map(_.barcode))
        } yield products.length
      )
      res must beEqualTo(2).await
    }

    "should deduct quantity - 1" in new TestScope {
      val p = createProduct
      val res = db.run(
        for {
          _       <- productRepo.insert(p.copy(qty = 10))
          _       <- productRepo.deductQuantity(p.barcode, 5)
          product <- productRepo.get(p.barcode)
        } yield product.map(_.qty)
      )
      res must beEqualTo(Some(5)).await
    }

    "should deduct quantity - 2" in new TestScope {
      val p = createProduct
      val res = db.run(
        for {
          _       <- productRepo.insert(p.copy(qty = 5))
          _       <- productRepo.deductQuantity(p.barcode, 5)
          product <- productRepo.get(p.barcode)
        } yield product.map(_.qty)
      )
      res must beEqualTo(Some(0)).await
    }

    "should add quantity" in new TestScope {
      val p = createProduct
      val res = db.run(
        for {
          _       <- productRepo.insert(p.copy(qty = 10))
          _       <- productRepo.addQuantity(p.barcode, 5)
          product <- productRepo.get(p.barcode)
        } yield product.map(_.qty)
      )
      res must beEqualTo(Some(15)).await
    }

    "should batch insert" in new TestScope {
      val ps = (1 to 5).map(i => createProduct)
      val res = db.run(
        for {
          _        <- productRepo.batchInsert(ps)
          products <- productRepo.findAll(ps.map(_.barcode))
        } yield products.length
      )
      res must beEqualTo(5).await
    }

    "should search for products" in new TestScope {
      val ps = (1 to 5).map(i => createProduct)
      val res = db.run(
        for {
          _  <- productRepo.batchInsert(ps)
          p1 <- productRepo.search(ps(0).barcode)
          p3 <- productRepo.search(ps(1).sku)
        } yield (p1, p3)
      )
      res.map(_._1.map(_.copy(id = ProductID.zero)).head) must beEqualTo(rowToDTO(ps(0))).await
      res.map(_._2.map(_.copy(id = ProductID.zero)).head) must beEqualTo(rowToDTO(ps(1))).await
    }

    "should create a product" in new TestScope {
      val product = createProduct

      val res = db.run(
        for {
          (b, c, _) <- insertTestData
          _         <- productRepo.create(product.copy(brandId = Some(b.id), categoryId = Some(c.id)))
          p         <- productRepo.get(product.barcode)
        } yield p
      )

      res.map(_.isDefined) must beTrue.await
    }

    "should filter products by brand" in new TestScope {
      val products = (1 to 20).map(_ => createProduct)

      val res = db.run(
        for {
          (b, c, p) <- insertTestData
          _ <- productRepo.batchInsert(products)
          ps <-productRepo.getAll(ProductFilters(brandId = Some(b.id)))
        } yield (ps, p)
      )
      res.map{ case (p1, p2) => p1.map(_.barcode) === p2.map(_.barcode)}.await
    }

    "should filter products by category" in new TestScope {
      val products = (1 to 20).map(_ => createProduct)

      val res = db.run(
        for {
          (b, c, p) <- insertTestData
          _ <- productRepo.batchInsert(products)
          ps <-productRepo.getAll(ProductFilters(categoryId = Some(c.id)))
        } yield (ps, p)
      )
      res.map{ case (p1, p2) => p1.map(_.barcode) === p2.map(_.barcode)}.await
    }
  }

  class TestScope extends Scope {
    val productRepo = ProductRepo(schema)

    def insertTestData = {
      import schema._
      import schema.profile.api._
      val sampleProducts = (1 to 2).map(i => createProduct).sortBy(_.barcode)
      for {
        brand    <- brands returning brands += BrandRow("b1")
        category <- categories returning categories += CategoryRow("c1")
        product <- products returning products ++= sampleProducts.map(
          _.copy(
            brandId = Some(brand.id),
            categoryId = Some(category.id)
          )
        )
      } yield (brand, category, product)
    }
  }
}
