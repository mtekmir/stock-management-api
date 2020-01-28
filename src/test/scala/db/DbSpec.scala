package db

import org.specs2.mutable.Specification
import slick.jdbc.PostgresProfile.api._
import db.Schema
import db.DbProfile
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import org.specs2.specification.AfterAll
import pureconfig._
import pureconfig.generic.auto._
import com.typesafe.config.ConfigFactory
import com.merit.db.DbSetup
import org.specs2.specification.BeforeAll
import com.merit.DbConfig

// Not used anymore
trait DbSpec extends Specification with BeforeAll with AfterAll {
  private val dbSettings = loadConfigOrThrow[DbConfig](ConfigFactory.load, "db")

  val dbSetup = DbSetup(dbSettings)
  val db      = dbSetup.connect()
  val schema  = Schema(DbProfile)

  override def beforeAll(): Unit =
    dbSetup.migrate()

  override def afterAll() =
    db.close()

  def run[R, S <: slick.dbio.NoStream, E <: slick.dbio.Effect](
    action: DBIOAction[R, S, E]
  )(implicit ec: ExecutionContext): Future[R] = {
    case class IntentionalRollbackException(successResult: R)
        extends Exception("Rolling back transaction")
    val block = action.flatMap(r => DBIO.failed(IntentionalRollbackException(r)))

    val tryResult = db.run(block.transactionally.asTry)

    tryResult.map {
      case Failure(IntentionalRollbackException(successResult)) => successResult
      case Failure(t)                                           => throw t
      case Success(r)                                           => r
    }
  }
}
