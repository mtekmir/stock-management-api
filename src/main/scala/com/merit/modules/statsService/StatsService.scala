package com.merit.modules.statsService

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIO
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.merit.modules.products.Currency
import org.joda.time.DateTimeFieldType
import org.joda.time.Weeks
import org.joda.time.Days
import org.joda.time.DateTime
import java.time.LocalTime
import java.util.Calendar
import org.joda.time.Months

trait StatsService {
  def getTopSellingProducts(
    filters: StatsDateFilter
  ): Future[Seq[TopSellingProduct]]
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

      def revenueChartData(
        filters: StatsDateFilter,
        chartOption: ChartOption.Value = ChartOption.Daily
      ): Future[Seq[Point]] = {
        import filters._
        import ChartOption._
        implicit class DTimeOps(d: DateTime) {
          val cal = Calendar.getInstance()
          def getWeek: Int = {
            cal.setTimeInMillis(d.getMillis)
            cal.get(Calendar.WEEK_OF_YEAR)
          }
          def getMonth: Int = {
            cal.setTimeInMillis(d.getMillis)
            cal.get(Calendar.MONTH)
          }
          def isSameMonth(d2: DateTime): Boolean =
            d.getMonth == d2.getMonth && d.getYear == d2.getYear

          def isSameWeek(d2: DateTime): Boolean =
            d.getWeek == d2.getWeek && d.getYear == d2.getYear

          def isSameDay(d2: DateTime): Boolean =
            d.getDayOfYear == d2.getDayOfYear && d.getYear == d2.getYear
        }

        def getYearDiff(start: DateTime, end: DateTime, times: Int): Int =
          ((end.getYear - start.getYear) * times)

        def getDayDiff(start: DateTime, end: DateTime): Int =
          end.getDayOfYear - start.getDayOfYear + getYearDiff(start, end, 365)

        def getWeekDiff(start: DateTime, end: DateTime): Int =
          end.getWeek - start.getWeek + getYearDiff(start, end, 52)

        def getMonthDiff(start: DateTime, end: DateTime): Int =
          end.getMonth - start.getMonth + getYearDiff(start, end, 12)

        db.run(
          statsRepo
            .getSalesData(filters)
            .map(sales => {
              chartOption match {
                case Daily =>
                  val differenceInDays = getDayDiff(startDate, endDate)
                  (0 until differenceInDays).map(dayIdx => {
                    val dayOfYear  = startDate.plusDays(dayIdx)
                    val salesOfDay = sales.filter(_.createdAt.isSameDay(dayOfYear))

                    Point(
                      dayOfYear.toString("MMM dd, YYYY"),
                      Currency(salesOfDay.map(_.total.value).sum)
                    )
                  })
                case Weekly =>
                  val differenceInWeek = getWeekDiff(startDate, endDate)
                  (0 until differenceInWeek).map(weekIdx => {
                    val week        = startDate.plusWeeks(weekIdx)
                    val salesOfWeek = sales.filter(_.createdAt.isSameWeek(week))

                    Point(
                      "Week of " + week.toString("MMM dd, YYYY"),
                      Currency(salesOfWeek.map(_.total.value).sum)
                    )
                  })
                case Monthly =>
                  val differenceInMonths = getMonthDiff(startDate, endDate)
                  (0 to differenceInMonths).map(monthIdx => {
                    val month        = startDate.plusMonths(monthIdx)
                    val salesOfMonth = sales.filter(_.createdAt.isSameMonth(month))
                    println(s"monthIdx: $monthIdx")
                    println(month.getMonth)
                    println(sales.map(_.createdAt.getMonth))
                    Point(
                      month.toString("MMM, YYYY"),
                      Currency(salesOfMonth.map(_.total.value).sum)
                    )
                  })
              }
            })
        )
      }
    }
}
