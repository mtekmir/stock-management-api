package com.merit.modules.stockOrders

import db.Schema
import com.merit.modules.products.OrderedProductRow
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow

trait StockOrderRepo[DbTask[_]] {
  def add(row: StockOrderRow): DbTask[StockOrderRow]
  def addProductsToStockOrder(
    products: Seq[OrderedProductRow]
  ): DbTask[Seq[OrderedProductRow]]
  def get(
    id: StockOrderID
  ): DbTask[Seq[(StockOrderRow, ProductRow, Int, Option[BrandRow], Option[CategoryRow])]]
}

object StockOrderRepo {
  def apply(schema: Schema): StockOrderRepo[slick.dbio.DBIO] =
    new StockOrderRepo[slick.dbio.DBIO] {
      import schema._
      import schema.profile.api._

      def add(row: StockOrderRow): DBIO[StockOrderRow] =
        stockOrders returning stockOrders += row

      def addProductsToStockOrder(
        products: Seq[OrderedProductRow]
      ): DBIO[Seq[OrderedProductRow]] =
        orderedProducts returning orderedProducts ++= products

      def get(id: StockOrderID): slick.dbio.DBIO[Seq[
        (StockOrderRow, ProductRow, Int, Option[BrandRow], Option[CategoryRow])
      ]] =
        stockOrders
          .filter(_.id === id)
          .join(orderedProducts)
          .on(_.id === _.stockOrderId)
          .join(products)
          .on {
            case ((so, op), p) => op.productId === p.id
          }
          .joinLeft(brands)
          .on {
            case (((so, op), p), b) => p.brandId === b.id
          }
          .joinLeft(categories)
          .on {
            case ((((so, op), p), b), c) => p.categoryId === c.id
          }
          .map {
            case ((((so, op), p), b), c) => (so, p, op.qty, b, c)
          }
          .result
    }
}
