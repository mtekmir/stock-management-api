package modules.brands

import org.specs2.concurrent.ExecutionEnv
import db.DbSpec
import org.specs2.specification.Scope
import org.specs2.matcher.FutureMatchers
import com.merit.modules.brands.BrandRepo
import com.merit.modules.brands.BrandRow

class BrandsRepSpec(implicit ee: ExecutionEnv) extends DbSpec with FutureMatchers {

  "BrandRepo" >> {
    "should get all brands" in new TestScope {
      val brands = run(for {
        _      <- insertTestData
        brands <- brandRepo.getAll
      } yield brands.map(_.name))
      brands must beEqualTo(Seq("brand1", "brand2")).await
    }

    "should add a new brand" in new TestScope {
      val brands = run(
        for {
          _      <- brandRepo.insert(BrandRow("newBrand"))
          brands <- brandRepo.getAll
        } yield brands.map(_.name)
      )
      brands must beEqualTo(Seq("newBrand")).await
    }

    "Should get a brand by name" in new TestScope {
      val brand = run(
        for {
          _     <- insertTestData
          brand <- brandRepo.getByName("brand1")
        } yield brand.map(_.name)
      )
      brand must beEqualTo(Some("brand1")).await
    }

    "Should batch insert brands" in new TestScope {
      val brands = run(
        for {
          _ <- brandRepo.batchInsert(
            Seq(BrandRow("brand11"), BrandRow("brand22"), BrandRow("brand33"))
          )
          brands <- brandRepo.getAll
        } yield brands.map(_.name)
      )
      brands must beEqualTo(Seq("brand11", "brand22", "brand33")).await
    }
  }

  class TestScope extends Scope {
    val brandRepo = BrandRepo(schema)
    import schema._
    import schema.profile.api._

    val insertTestData: DBIO[Unit] = for {
      _ <- brands ++= Seq(BrandRow("brand1"), BrandRow("brand2"))
    } yield ()
  }
}
