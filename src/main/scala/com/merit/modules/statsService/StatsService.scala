package com.merit.modules.statsService

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIO
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.merit.modules.products.Currency
import org.joda.time.DateTime
import java.lang.Math.floorMod
import com.merit.modules.sales.SaleStatus
import com.merit.modules.sales.SaleRow
import com.merit.modules.sales.SaleOutlet

trait StatsService {
  def getTopSellingProducts(
    page: Int,
    rowsPerPage: Int,
    filters: StatsDateFilter
  ): Future[PaginatedTopSellingProducts]
  def getInventorySummary(): Future[InventorySummary]
  def getStats(filters: StatsDateFilter): Future[Stats]
  def revenueChartData(
    filters: StatsDateFilter,
    chartOption: ChartOption.Value = ChartOption.Daily
  ): Future[Seq[Point]]
}

object StatsService {
  def apply(db: Database, statsRepo: StatsRepo[DBIO])(implicit ec: ExecutionContext) =
    new StatsService {
      def getTopSellingProducts(
        page: Int,
        rowsPerPage: Int,
        filters: StatsDateFilter
      ): Future[PaginatedTopSellingProducts] =
        db.run(
          for {
            count    <- statsRepo.count(filters)
            products <- statsRepo.getTopSellingProducts(page, rowsPerPage, filters)
          } yield
            PaginatedTopSellingProducts(count, products.map {
              case (sku, name, variation, soldQty) =>
                TopSellingProduct(sku, name, variation, soldQty)
            })
        )

      def getInventorySummary(): Future[InventorySummary] =
        db.run(statsRepo.getInventorySummary().map {
          case (currentStock, stockValue) =>
            InventorySummary(currentStock, Currency(stockValue))
        })

      def getStats(filters: StatsDateFilter): Future[Stats] =
        db.run(statsRepo.getStats(filters).map {
          case (web, store, saleCount, productsSold) =>
            Stats(
              Currency(web.getOrElse(0)),
              Currency(store.getOrElse(0)),
              saleCount,
              productsSold
            )
        })

      def revenueChartData(
        filters: StatsDateFilter,
        chartOption: ChartOption.Value = ChartOption.Daily
      ): Future[Seq[Point]] = {
        import filters._
        import ChartOption._

        val statusesToCount = SaleStatus.fulfilledSaleStatuses

        def getYearDiff(start: DateTime, end: DateTime, times: Int): Int =
          ((end.getYear - start.getYear) * times)

        def getDayDiff(start: DateTime, end: DateTime): Int =
          end.getDayOfYear - start.getDayOfYear + getYearDiff(start, end, 365)

        def getWeekDiff(start: DateTime, end: DateTime): Int =
          end.getWeek - start.getWeek + getYearDiff(start, end, 52)

        def getMonthDiff(start: DateTime, end: DateTime): Int =
          end.getMonth - start.getMonth + getYearDiff(start, end, 12)

        def calcTotal(sales: Seq[SaleRow]): Currency = Currency(sales.map(_.total.value).sum)
        def toPoint(date: String, sales: Seq[SaleRow]): Point =
          Point(
            date,
            calcTotal(sales.filter(_.outlet == SaleOutlet.Web)),
            calcTotal(sales.filter(_.outlet == SaleOutlet.Store))
          )

        db.run(
          statsRepo
            .getSalesData(filters)
            .map(sales => {
              chartOption match {
                case Daily =>
                  val differenceInDays = getDayDiff(startDate, endDate)
                  (0 to differenceInDays).map(dayIdx => {
                    val dayOfYear = startDate.plusDays(dayIdx)
                    val salesOfDay = sales.filter(
                      s =>
                        s.createdAt.isSameDay(dayOfYear) && statusesToCount.contains(s.status)
                    )

                    toPoint(dayOfYear.toString("MMM dd, YYYY"), salesOfDay)
                  })
                case Weekly =>
                  val differenceInWeek = getWeekDiff(startDate, endDate)
                  (0 to differenceInWeek).map(weekIdx => {
                    val weekNum = floorMod(startDate.getWeek + weekIdx, 52) match {
                      case 0      => 52
                      case n: Int => n
                    }
                    val week = startDate.withDayOfWeek(1).plusWeeks(weekIdx)
                    val salesOfWeek = sales.filter(
                      s => s.createdAt.getWeek == weekNum && statusesToCount.contains(s.status)
                    )

                    toPoint("Week of " + week.toString("MMM dd, YYYY"), salesOfWeek)
                  })
                case Monthly =>
                  val differenceInMonths = getMonthDiff(startDate, endDate)
                  (0 to differenceInMonths).map(monthIdx => {
                    val month = startDate.withDayOfMonth(1).plusMonths(monthIdx)
                    val salesOfMonth = sales.filter(
                      s => s.createdAt.isSameMonth(month) && statusesToCount.contains(s.status)
                    )

                    toPoint(month.toString("MMM, YYYY"), salesOfMonth)
                  })
              }
            })
        )
      }
    }
}
