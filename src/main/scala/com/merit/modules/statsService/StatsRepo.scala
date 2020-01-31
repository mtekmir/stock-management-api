package com.merit.modules.statsService

import db.Schema

trait StatsRepo[DbTask[_]] {
  def getTopSellingProducts(): DbTask[Seq[(String, String, Option[String], Int)]]
  def getInventorySummary(): DbTask[InventorySummary]
}

object StatsRepo {
  def apply(schema: Schema) = new StatsRepo[slick.dbio.DBIO] {
    import schema._
    import schema.profile.api._
    
    def getTopSellingProducts(): DBIO[Seq[(String, String, Option[String], Int)]] = 
      sql"""
        SELECT sku, name, variation,
        SUM(sold_products.qty) AS unitSold
        FROM sold_products
        JOIN products
        ON sold_products."productId" = products.id
        GROUP BY products.id
        ORDER BY unitSold DESC
        LIMIT 6
      """.as[(String, String, Option[String], Int)]

    def getInventorySummary(): DBIO[InventorySummary] = ???
      // sql"""
      //   SELECT COUNT(qty) AS currentStock, 
      //   SUM(qty * price) AS stockValue
      //   FROM products p
      // """.as[()]

  }
}