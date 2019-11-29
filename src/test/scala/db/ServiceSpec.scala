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
import pureconfig._
import pureconfig.generic.auto._
import com.merit.db.DbSettings
import com.merit.db.Db

trait ServiceSpec extends Specification with BeforeAll with AfterAll {
  private val dbname =
    getClass.getSimpleName.toLowerCase
  private val driver =
    "org.postgresql.Driver"

  private val dbSettings = loadConfigOrThrow[DbSettings](ConfigFactory.load, "db")

  val db1 = Db(dbSettings)
  var db  = db1

  def exec[R](db: Database)(dbio: DBIO[R]) = Await.result(db.run(dbio), Duration.Inf)
  val schema                               = Schema(DbProfile)

  override def beforeAll() = {
    exec(db)(sqlu"""drop database if exists #$dbname""")
    exec(db)(sqlu"""create database #$dbname""")
    db = Db(dbSettings.copy(url = s"jdbc:postgresql://localhost:5434/$dbname"))
    schema.createTables(db)(scala.concurrent.ExecutionContext.global)
  }

  override def afterAll() = {
    db.close()
    exec(db1)(sqlu"""drop database #$dbname""")
    db1.close()
  }
  sequential
}
