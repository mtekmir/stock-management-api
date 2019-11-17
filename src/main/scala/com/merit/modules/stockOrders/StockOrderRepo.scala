package com.merit.modules.stockOrders

import db.Schema
import com.merit.modules.products.OrderedProductRow

trait StockOrderRepo[DbTask[_]] {
  def add(row: StockOrderRow): DbTask[StockOrderID]
  def addProductsToStockOrder(products: Seq[OrderedProductRow]): DbTask[Seq[OrderedProductRow]]
}

object StockOrderRepo {
  def apply(schema: Schema): StockOrderRepo[slick.dbio.DBIO] = new StockOrderRepo[slick.dbio.DBIO] {
    import schema._
    import schema.profile.api._

    def add(row: StockOrderRow): DBIO[StockOrderID] = 
      stockOrders returning stockOrders.map(_.id) += row

    def addProductsToStockOrder(products: Seq[OrderedProductRow]): DBIO[Seq[OrderedProductRow]] =
      orderedProducts returning orderedProducts ++= products
  }
}