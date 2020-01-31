package com.merit.modules.statsService

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIO
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait StatsService {
  def getTopSellingProducts(): Future[Seq[TopSellingProduct]]
}

object StatsService {
  def apply(db: Database, statsRepo: StatsRepo[DBIO])(implicit ec: ExecutionContext) =
    new StatsService {
      def getTopSellingProducts(): Future[Seq[TopSellingProduct]] =
        db.run(statsRepo.getTopSellingProducts().map {
          _.map {
            case (sku, name, variation, soldQty) =>
              TopSellingProduct(sku, name, variation, soldQty)
          }
        })
    }
}
