package com.merit.modules.salesEvents

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.dbio.DBIO

trait SaleEventService {
  def getEvents(limit: Int): Future[Seq[SaleEventDTO]]
}

object SaleEventService {
  def apply(db: Database, saleEventRepo: SaleEventRepo[DBIO])(implicit ec: ExecutionContext) =
    new SaleEventService {
      def getEvents(limit: Int): Future[Seq[SaleEventDTO]] =
        db.run(saleEventRepo.getAll(limit)).map {
          _.map {
            case (event, user) => event.toDTO(user)
          }
        }
    }
}
