package com.merit.modules.statsService

import db.Schema
import scala.concurrent.ExecutionContext
import org.joda.time.DateTime
import com.merit.modules.sales.SaleRow

trait StatsRepo[DbTask[_]] {
  def count(
    filters: StatsDateFilter
  ): DbTask[Int]
  def getTopSellingProducts(
    page: Int,
    rowsPerPage: Int,
    filters: StatsDateFilter
  ): DbTask[Seq[(String, String, Option[String], Int)]]
  def getInventorySummary(): DbTask[(Int, BigDecimal)]
  def getStats(
    filters: StatsDateFilter
  ): DbTask[(Option[BigDecimal], Option[BigDecimal], Int, Int)]
  def getSalesData(filters: StatsDateFilter): DbTask[Seq[SaleRow]]
}

object StatsRepo {
  def apply(schema: Schema)(implicit ec: ExecutionContext) = new StatsRepo[slick.dbio.DBIO] {
    import schema._
    import schema.profile.api._

    def formatDate(d: DateTime) = d.toString("YYYY/MM/dd")
    def getFilterQuery(filter: StatsDateFilter): String = {
      import filter._
      s"""
        WHERE sales.created >= '${formatDate(startDate)}' 
        AND sales.created <= '${formatDate(endDate)}'
      """
    }

    def count(filters: StatsDateFilter): DBIO[Int] = {
      import filters._
      sales
        .filter(s => s.createdAt >= startDate && s.createdAt <= endDate)
        .join(soldProducts)
        .on(_.id === _.saleId)
        .length
        .result
    }

    def getTopSellingProducts(
      page: Int,
      rowsPerPage: Int,
      filters: StatsDateFilter
    ): DBIO[Seq[(String, String, Option[String], Int)]] =
      sql"""
        SELECT sku, name, variation,
        SUM(sold_products.qty) AS unitSold
        FROM sold_products
        JOIN sales 
        ON sales.id = sold_products."saleId"
        JOIN products
        ON sold_products."productId" = products.id
        #${getFilterQuery(filters)}
        GROUP BY products.id
        ORDER BY unitSold DESC
        LIMIT #$rowsPerPage
        OFFSET #${(page - 1) * rowsPerPage}
      """.as[(String, String, Option[String], Int)]

    def getInventorySummary(): DBIO[(Int, BigDecimal)] =
      sql"""
        SELECT COUNT(qty) AS currentStock, 
        SUM(qty * price) AS stockValue
        FROM products
        WHERE qty >= 0
      """.as[(Int, BigDecimal)].head

    def getStats(
      filter: StatsDateFilter
    ): DBIO[(Option[BigDecimal], Option[BigDecimal], Int, Int)] = {
      val filters = getFilterQuery(filter)

      sql"""
        select (select sum(total) from sales #${filters} and outlet = 'Web') as webRevenue,
        (select sum(total) from sales #${filters} and outlet = 'Store') as storeRevenue,
        (select count(sales.id) from sales #${filters}) as saleCount,
        (select sum(sold_products.qty) 
        	from sold_products 
          join sales on sold_products."saleId" = sales.id
          #${filters}
        ) as productsSold
      """.as[(Option[BigDecimal], Option[BigDecimal], Int, Int)].head
    }

    def getSalesData(filters: StatsDateFilter): DBIO[Seq[SaleRow]] =
      sales
        .filter(s => s.createdAt >= filters.startDate && s.createdAt <= filters.endDate)
        .result
  }
}
