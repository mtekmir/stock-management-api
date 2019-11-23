package db

import org.specs2.mutable.Specification
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.specs2.specification.BeforeAll
import org.specs2.specification.AfterAll
import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile

trait ServiceSpec extends Specification with BeforeAll with AfterAll {
  private val dbname =
    getClass.getSimpleName.toLowerCase
  private val driver =
    "org.postgresql.Driver"

  
  val db1 = Database.forConfig("db")
  var db = db1

  def exec[R](db: Database)(dbio: DBIO[R]) = Await.result(db.run(dbio), Duration.Inf)
  val schema = Schema(DbProfile)

  override def beforeAll() = {
    println("beforeall")
    exec(db)(sqlu"""drop database if exists #$dbname""")
    exec(db)(sqlu"""create database #$dbname""")
    db = Database.forURL(
      s"jdbc:postgresql://localhost:5434/$dbname",
      user = "postgres",
      password = "postgres",
      driver = driver
    )

    schema.createTables(db)(scala.concurrent.ExecutionContext.global)
  }

  override def afterAll() = {
    println("afterall")
    exec(db1)(sqlu"""drop database #$dbname""")
    db.close()
    db1.close()
  }
  sequential
}
