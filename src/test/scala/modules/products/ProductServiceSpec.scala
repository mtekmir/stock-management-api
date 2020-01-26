package modules.products

import db.DbSpecification
import org.specs2.matcher.FutureMatchers
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.Scope
import com.merit.modules.products.ProductRepo
import com.merit.modules.brands.BrandRepo
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.products.ProductService
import utils.ProductUtils._
import utils.ExcelTestUtils.getExcelProductRows
import com.merit.modules.products.ProductID
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import org.specs2.specification.AfterEach
import com.merit.modules.products.EditProductRequest

class ProductServiceSpec(implicit ee: ExecutionEnv)
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

  "Product Service" >> {
    "should get a product by barcode" in new TestScope {
      val p = createProduct
      val res = for {
        _       <- db.run(productRepo.insert(p))
        product <- productService.getProduct(p.barcode)
      } yield product.map(_.copy(id = ProductID.zero))
      res must beEqualTo(Some(rowToDTO(p))).await
    }

    "should batch insert excel rows" in new TestScope {
      val rows = getExcelProductRows(5).sortBy(_.barcode)

      val res = for {
        _          <- productService.batchInsertExcelRows(rows)
        products   <- productService.findAll(rows.map(_.barcode))
        brands     <- db.run(brandRepo.getAll)
        categories <- db.run(categoryRepo.getAll)
      } yield (products.sortBy(_.barcode), brands, categories)
      res.map(_._1.map(_.barcode)) must beEqualTo(rows.map(_.barcode)).await
      res.map(_._1.map(_.name)) must beEqualTo(rows.map(_.name)).await
      res.map(_._1.map(_.qty)) must beEqualTo(rows.map(_.qty)).await
      res.map(_._1.map(_.variation)) must beEqualTo(rows.map(_.variation)).await
      res.map(_._2.map(_.name)) must beEqualTo(rows.map(_.brand).distinct.flatten).await
      res.map(_._3.map(_.name)) must beEqualTo(rows.map((_.category)).distinct.flatten).await
    }

    "should get all the products" in new TestScope {
      val rows = getExcelProductRows(5).sortBy(_.barcode)

      val res = for {
        _        <- productService.batchInsertExcelRows(rows)
        products <- productService.findAll(rows.map(_.barcode))
      } yield (sortedWithZeroIdProductDTO(products))
      res must beEqualTo(rows.map(r => excelRowToDTO(r))).await
    }

    "should add quantities to a batch" in new TestScope {
      val rows = getExcelProductRows(5).sortBy(_.barcode)

      val res = for {
        _        <- productService.batchInsertExcelRows(rows)
        _        <- productService.batchAddQuantity(rows.map(r => (r.barcode, 5)))
        products <- productService.findAll(rows.map(_.barcode))
      } yield (sortedWithZeroIdProductDTO(products))
      res must beEqualTo(rows.map(p => excelRowToDTO(p.copy(qty = p.qty + 5)))).await
    }

    "should add quantities to a batch - 2" in new TestScope {
      val rows   = getExcelProductRows(5).sortBy(_.barcode)
      val qtys   = rows.zipWithIndex.map(r => (r._1.barcode, r._2))
      val qtyMap = qtys.foldLeft(Map[String, Int]())((m, r) => m + (r._1 -> r._2))

      val res = for {
        _        <- productService.batchInsertExcelRows(rows)
        _        <- productService.batchAddQuantity(qtys)
        products <- productService.findAll(rows.map(_.barcode))
      } yield (sortedWithZeroIdProductDTO(products))
      res must beEqualTo(rows.map(p => excelRowToDTO(p.copy(qty = p.qty + qtyMap(p.barcode))))).await
    }

    "should get all products with pagination" in new TestScope {
      val rows = getExcelProductRows(15).sortBy(_.barcode)
      val dtos = rows.map(excelRowToDTO(_))
      val res = for {
        _         <- productService.batchInsertExcelRows(rows)
        products1 <- productService.getProducts(1, 5)
        products2 <- productService.getProducts(2, 5)
        products3 <- productService.getProducts(3, 5)
      } yield (products1, products2, products3)
      res.map(_._1.count) must beEqualTo(15).await
      res.map(r => sortedWithZeroIdProductDTO(r._1.products)) must beEqualTo(dtos.take(5)).await
      res.map(r => sortedWithZeroIdProductDTO(r._2.products)) must beEqualTo(
        dtos.drop(5).take(5)
      ).await
      res.map(r => sortedWithZeroIdProductDTO(r._3.products)) must beEqualTo(
        dtos.drop(10).take(5)
      ).await

    }

    "should create a product" in new TestScope {
      val p = createProduct
      val res = for {
        _       <- productService.createProduct(p.toCreateProductReq)
        product <- productService.getProduct(p.barcode)
      } yield (product)
      res.map(_.map(_.copy(id = ProductID.zero))) must beEqualTo(Some(rowToDTO(p))).await
    }

    "should error out on duplicate barcode on create product req" in new TestScope {
      val res = for {
        _   <- productService.createProduct(sampleProducts.head.toCreateProductReq)
        res <- productService.createProduct(sampleProducts.head.toCreateProductReq)
      } yield (res)
      res must beEqualTo(Left(s"Barcode ${sampleProducts.head.barcode} already exists")).await
    }

    "should error out on duplicate barcode on edit product req" in new TestScope {
      val res = for {
        p <- productService.createProduct(sampleProducts.head.toCreateProductReq)
        res <- productService.editProduct(
          p.map(_.id).getOrElse(ProductID.zero),
          EditProductRequest(Some(sampleProducts.head.barcode), categoryId = Some("1"))
        )
      } yield (res)
      res must beEqualTo(Left("Barcode already exists")).await
    }

    "should edit product" in new TestScope {
      val product = sampleProducts.head
      val edited = sampleProducts.tail.head
      val res = for {
        p <- productService.createProduct(product.toCreateProductReq)
        res <- productService.editProduct(
          p.map(_.id).getOrElse(ProductID.zero),
          edited.toEditProductReq
        )
        updated <- db.run(productRepo.get(p.map(_.id).getOrElse(ProductID.zero)))
      } yield updated

      res.map(_.map(_.copy(id=ProductID.zero))) must beEqualTo(Some(rowToDTO(edited))).await
    }
  }

  class TestScope extends Scope {
    val productRepo  = ProductRepo(schema)
    val brandRepo    = BrandRepo(schema)
    val categoryRepo = CategoryRepo(schema)

    val productService = ProductService(db, brandRepo, productRepo, categoryRepo)
    val sampleProducts = (1 to 5).map(i => createProduct)
  }
}
