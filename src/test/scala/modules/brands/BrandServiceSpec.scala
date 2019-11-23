package modules.brands

import db.ServiceSpec
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.Scope
import com.merit.modules.brands.BrandRepo
import com.merit.modules.brands.BrandService
import org.specs2.concurrent.ExecutionEnv
import com.merit.modules.brands.BrandRow
import utils.ExcelTestUtils._
import org.specs2.specification.AfterEach

class BrandServiceSpec(implicit ee: ExecutionEnv)
    extends ServiceSpec
    with FutureMatchers with AfterEach {
    override def after = {
      import schema._
      import schema.profile.api._
      db.run(brands.delete)
    }

  "Brand service" >> {
    "should get all brands" in new TestScope {
      val brands = for {
        _      <- insertTestData
        brands <- brandService.getAll
      } yield brands.map(_.name)
      brands must beEqualTo(Seq("brand1", "brand2")).await
    }

    "should insert a brand" in new TestScope {
      val brands = for {
        _      <- brandService.insert(BrandRow("testBrand"))
        brands <- brandService.getAll
      } yield brands.map(_.name)
      brands must beEqualTo(Seq("testBrand")).await
    }

    "should batch insert brands" in new TestScope {
      
      val brandNames = Seq("test1", "test2")
      val res = for {
        _ <- brandService.batchInsert(brandNames.map(BrandRow(_)))
        brands <- brandService.getAll
      } yield brands.map(_.name)
      res must beEqualTo(brandNames).await
    }

    "should not insert the same brand twice" in new TestScope {
      val brandName = "test"
      val res = for {
        _ <- brandService.insert(BrandRow(brandName))
        _ <- brandService.insert(BrandRow(brandName))
        brands <- brandService.getAll
      } yield brands.map(_.name)
      res must beEqualTo(Seq(brandName)).await
    }
  } 

  class TestScope extends Scope {
    val brandRepo    = BrandRepo(schema)
    val brandService = BrandService(db, brandRepo)

    def insertTestData = {
      import schema._
      import schema.profile.api._

      db.run(for {
        _ <- brands ++= Seq(BrandRow("brand1"), BrandRow("brand2"))
      } yield ())
    }
  }
}
