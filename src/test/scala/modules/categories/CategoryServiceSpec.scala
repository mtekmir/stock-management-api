package modules.categories
import db.DbSpecification
import org.specs2.matcher.FutureMatchers
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.AfterEach
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.categories.CategoryService
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import org.specs2.specification.Scope
import utils.TestUtils._
import utils.ExcelTestUtils

class CategoryServiceSpec(implicit ee: ExecutionEnv)
    extends DbSpecification
    with FutureMatchers
    with AfterEach {
  override def after: Any = {
    import schema._
    import schema.profile.api._
    db.run(categories.delete)
  }

  "Category Service" >> {
    "should get all categories" in new TestScope {
      val res = for {
        _ <- insertTestData
        categories <- categoryService.getCategories
      } yield categories.map(_.name)
      res must beEqualTo(Seq("test1", "test2")).await
    }

    "should insert a category" in new TestScope {
      val res = for {
        _ <- categoryService.create("New")
        categories <- categoryService.getCategories
      } yield categories.map(_.name)
      res must beEqualTo(Seq("New")).await
    }

    "should not insert the same category twice" in new TestScope {
      val res = for {
        _ <- categoryService.create("New")
        _ <- categoryService.create("New")
        categories <- categoryService.getCategories
      } yield categories.map(_.name)
      res must beEqualTo(Seq("New")).await
    }

    "batch inserts categories" in new TestScope {
      val categoryNames = Seq("c0", "c01", "c03")
      val res = for {
        _ <- categoryService.batchInsert(categoryNames)
        categories <- categoryService.getCategories
      } yield categories.map(_.name)
      res must beEqualTo(categoryNames).await
    }
  }

  class TestScope extends Scope {
    val categoryRepo    = CategoryRepo(schema)
    val categoryService = CategoryService(db, categoryRepo)

    def insertTestData = {
      import schema._
      import schema.profile.api._
      db.run(
        for {
          _ <- categories ++= Seq(CategoryRow("test1"), CategoryRow("test2"))
        } yield ()
      )
    }
  }
}
