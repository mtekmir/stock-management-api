import org.apache.poi.ss.usermodel._
import scala.jdk.CollectionConverters._
import db.{DbProfile, Schema}
import slick.dbio.DBIO
import scala.concurrent.Await
import scala.concurrent.duration._
import slick.jdbc.PostgresProfile.api._
import com.merit.modules.products.{ProductRepo, ProductService}
import com.merit.modules.brands.{BrandRepo, BrandService}
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.util.Failure
import scala.util.Success
import com.merit.modules.excel.ExcelService
import api.Router
import com.merit.modules.sales.{SaleRepo, SaleService}
import com.merit.modules.users.{UserRepo, UserService}
import com.merit.modules.categories.{CategoryRepo, CategoryService}
import com.typesafe.config.ConfigFactory
import com.merit.db.DbSettings
import pureconfig._
import pureconfig.generic.auto._
import com.merit.db.Db

object Main extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val system       = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private val dbSettings = loadConfigOrThrow[DbSettings](ConfigFactory.load, "db")

  val db     = Db(dbSettings)
  
  val schema = Schema(DbProfile)
  schema.createTables(db)
  val productRepo  = ProductRepo(schema)
  val brandRepo    = BrandRepo(schema)
  val saleRepo     = SaleRepo(schema)
  val userRepo     = UserRepo(schema)
  val categoryRepo = CategoryRepo(schema)

  val productService  = ProductService(db, brandRepo, productRepo, categoryRepo)
  val brandService    = BrandService(db, brandRepo)
  val categoryService = CategoryService(db, categoryRepo)
  val excelService    = ExcelService()
  val saleService     = SaleService(db, saleRepo, productRepo)
  val userService     = UserService(db, userRepo)

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2.seconds)

  exec(userService.populateUsers)

  val routes =
    Router(
      saleService,
      productService,
      brandService,
      excelService,
      userService,
      categoryService
    )

  Http().bindAndHandle(routes, "localhost", 3000).onComplete {
    case Success(_) => println("Server is op on 3000")
    case Failure(e) => println(e)
  }
}
