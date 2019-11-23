package modules.categories
import org.specs2.concurrent.ExecutionEnv
import db.DbSpec
import org.specs2.matcher.FutureMatchers
import org.specs2.specification.Scope
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.categories.CategoryRow
import utils.TestUtils._

class CategoryRepoSpec(implicit ee: ExecutionEnv) extends DbSpec with FutureMatchers {
  "Category Repo" >> {
    "should insert a category" in new TestScope {
      val res = run(
        for {
          _          <- categoryRepo.insert(CategoryRow("c1"))
          categories <- categoryRepo.getAll
        } yield categories.map(_.name)
      )
      res must beEqualTo(Seq("c1")).await
    }

    "should get a category by name" in new TestScope {
      val res = run(
        for {
          _        <- categoryRepo.insert(CategoryRow("c2"))
          category <- categoryRepo.getByName("c2")
        } yield category.map(_.name)
      )
      res must beEqualTo(Some("c2")).await
    }

    "should batch insert categories" in new TestScope {
      val names = Seq("category1", "category2", "category3")
      val res = run(
        for {
          _          <- categoryRepo.batchInsert(names.map(CategoryRow(_)))
          categories <- categoryRepo.getAll
        } yield categories.map(_.name)
      )
      res must beEqualTo(names).await
    }
  }

  class TestScope extends Scope {
    val categoryRepo = CategoryRepo(schema)
  }
}
