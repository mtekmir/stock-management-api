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
import com.merit.db.DbSetup
import com.merit.DbConfig
import scala.concurrent.duration._
import org.specs2.matcher.Matcher
import org.specs2.concurrent.ExecutionEnv

trait DbSpecification extends Specification with BeforeAll with AfterAll {
  private val dbname =
    getClass.getSimpleName.toLowerCase
  private val driver =
    "org.postgresql.Driver"

  private val dbSettings = loadConfigOrThrow[DbConfig](ConfigFactory.load, "db")

  val db1 = DbSetup(dbSettings).connect()
  var db  = db1

  def exec[R](db: Database)(dbio: DBIO[R]) = Await.result(db.run(dbio), Duration.Inf)
  val schema                               = Schema(DbProfile)

  override def beforeAll() = {
    exec(db)(sqlu"""drop database if exists #$dbname""")
    exec(db)(sqlu"""create database #$dbname""")
    val dbSetup = DbSetup(dbSettings.copy(url = s"jdbc:postgresql://localhost:5434/$dbname"))
    db = dbSetup.connect()
    dbSetup.migrate()
  }

  override def afterAll() = {
    db.close()
    exec(db1)(sqlu"""drop database #$dbname""")
    db1.close()
  }
  sequential
}
