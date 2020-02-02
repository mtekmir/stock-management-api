package com.merit.modules.statsService

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIO
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.merit.modules.products.Currency

trait StatsService {
  def getTopSellingProducts(
    filters: StatsDateFilter
  ): Future[Seq[TopSellingProduct]]
  def getInventorySummary(): Future[InventorySummary]
  def getStats(filters: StatsDateFilter): Future[Stats]
  def revenueChartData(filters: StatsDateFilter): Future[Seq[Point]]
}

object StatsService {
  def apply(db: Database, statsRepo: StatsRepo[DBIO])(implicit ec: ExecutionContext) =
    new StatsService {
      def getTopSellingProducts(
        filters: StatsDateFilter
      ): Future[Seq[TopSellingProduct]] =
        db.run(statsRepo.getTopSellingProducts(filters).map {
          _.map {
            case (sku, name, variation, soldQty) =>
              TopSellingProduct(sku, name, variation, soldQty)
          }
        })

      def getInventorySummary(): Future[InventorySummary] =
        db.run(statsRepo.getInventorySummary().map {
          case (currentStock, stockValue) =>
            InventorySummary(currentStock, Currency(stockValue))
        })

      def getStats(filters: StatsDateFilter): Future[Stats] =
        db.run(statsRepo.getStats(filters).map {
          case (rev, saleCount, productsSold) => Stats(Currency(rev), saleCount, productsSold)
        })

      def revenueChartData(filters: StatsDateFilter): Future[Seq[Point]] = ???
    }
}
