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
import scala.util.{Failure, Success}
import com.merit.modules.excel.ExcelService
import api.Router
import com.merit.modules.sales.{SaleRepo, SaleService}
import com.merit.modules.users.{UserRepo, UserService}
import com.merit.modules.categories.{CategoryRepo, CategoryService}
import com.typesafe.config.ConfigFactory
import com.merit.db.Db
import pureconfig._
import pureconfig.generic.auto._
import com.merit.modules.emails.{EmailServiceActor, EmailMessage}
import akka.actor.Props
import com.merit.modules.stockOrders.StockOrderRepo
import com.merit.modules.stockOrders.StockOrderService
import com.merit.external.crawler.CrawlerClient
import com.merit.modules.sales.{SaleSummary, SaleID, SaleSummaryProduct}
import com.merit.AwsConfig
import com.merit.external.sqsClient.SqsClient
import com.merit.modules.products.ProductID
import com.merit.{Config, AppConfig}

object Main extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val system       = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val appConfig @ AppConfig(
    awsConfig,
    dbConfig,
    emailConfig,
    crawlerClientConfig,
    _,
    httpConfig
  ) =
    Config().load()

  val db = Db(dbConfig)

  val schema = Schema(DbProfile)
  schema.createTables(db)
  val productRepo    = ProductRepo(schema)
  val brandRepo      = BrandRepo(schema)
  val saleRepo       = SaleRepo(schema)
  val userRepo       = UserRepo(schema)
  val categoryRepo   = CategoryRepo(schema)
  val stockOrderRepo = StockOrderRepo(schema)

  val sqsClient     = SqsClient(awsConfig)
  val crawlerClient = CrawlerClient(crawlerClientConfig, sqsClient)

  val productService  = ProductService(db, brandRepo, productRepo, categoryRepo)
  val brandService    = BrandService(db, brandRepo)
  val categoryService = CategoryService(db, categoryRepo)
  val excelService    = ExcelService()
  val saleService     = SaleService(db, saleRepo, productRepo, crawlerClient)
  val userService     = UserService(db, userRepo)
  val stockOrderService =
    StockOrderService(db, stockOrderRepo, productRepo, brandRepo, categoryRepo, crawlerClient)
  val emailService =
    system.actorOf(Props(new EmailServiceActor(emailConfig)), name = "EmailService")

  // crawlerClient.sendSale(
  //   SaleSummary(SaleID(1L), Seq(SaleSummaryProduct(ProductID.zero, "000000000004", "test", None, 2, 2)))
  // )

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2.seconds)

  exec(userService.populateUsers)

  val routes =
    Router(
      saleService,
      productService,
      brandService,
      excelService,
      userService,
      categoryService,
      stockOrderService,
      appConfig
    )

  Http()
    .bindAndHandle(routes, httpConfig.interface, httpConfig.port)
    .onComplete {
      case Success(_) => println(s"Server is up on ${httpConfig.port}")
      case Failure(e) => println(e)
    }
}
