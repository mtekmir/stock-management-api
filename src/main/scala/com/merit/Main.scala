import java.io.File

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

object Main extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val system       = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val db     = Database.forConfig("db")
  val schema = Schema(DbProfile)
  schema.createTables(db)
  val productRepo = ProductRepo(schema)
  val brandRepo   = BrandRepo(schema)
  val saleRepo    = SaleRepo(schema)
  val userRepo    = UserRepo(schema)

  val productService = ProductService(db, brandRepo, productRepo)
  val brandService   = BrandService(db, brandRepo)
  val excelService   = ExcelService()
  val saleService    = SaleService(db, saleRepo, productRepo)
  val userService    = UserService(db, userRepo)

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2.seconds)

  exec(userService.populateUsers)

  val routes =
    Router(saleService, productService, brandService, excelService, userService)

  Http().bindAndHandle(routes, "localhost", 3000).onComplete {
    case Success(_) => println("Server is op on 3000")
    case Failure(e) => println(e)
  }
}
