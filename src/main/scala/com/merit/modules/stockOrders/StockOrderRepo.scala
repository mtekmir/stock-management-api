package com.merit.modules.stockOrders

import db.Schema
import com.merit.modules.products.OrderedProductRow
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import com.merit.modules.products.ProductID

trait StockOrderRepo[DbTask[_]] {
  def insert(row: StockOrderRow): DbTask[StockOrderRow]
  def addProductsToStockOrder(
    products: Seq[OrderedProductRow]
  ): DbTask[Seq[OrderedProductRow]]
  def get(
    id: StockOrderID
  ): DbTask[
    Seq[(StockOrderRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow])]
  ]
  def getAll(
    ): DbTask[
    Seq[(StockOrderRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow])]
  ]
  def syncOrderedProduct(
    stockOrderId: StockOrderID,
    productId: ProductID,
    synced: Boolean
  ): DbTask[Int]
}

object StockOrderRepo {
  def apply(schema: Schema): StockOrderRepo[slick.dbio.DBIO] =
    new StockOrderRepo[slick.dbio.DBIO] {
      import schema._
      import schema.profile.api._

      implicit class SOOPS(
        val q: Query[StockOrderTable, StockOrderRow, Seq]
      ) {
        def withProducts =
          q.join(orderedProducts)
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
              case ((((so, op), p), b), c) => (so, p, op.qty, op.synced, b, c)
            }
      }

      def insert(row: StockOrderRow): DBIO[StockOrderRow] =
        stockOrders returning stockOrders += row

      def addProductsToStockOrder(
        products: Seq[OrderedProductRow]
      ): DBIO[Seq[OrderedProductRow]] =
        orderedProducts returning orderedProducts ++= products

      def get(id: StockOrderID): DBIO[Seq[
        (StockOrderRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow])
      ]] =
        stockOrders
          .filter(_.id === id)
          .withProducts
          .result

      def getAll(): DBIO[
        Seq[(StockOrderRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow])]
      ] =
        stockOrders.withProducts.result

      def syncOrderedProduct(
        stockOrderId: StockOrderID,
        productId: ProductID,
        synced: Boolean
      ): DBIO[Int] =
        orderedProducts
          .filter(p => p.stockOrderId === stockOrderId && p.productId === productId)
          .map(_.synced)
          .update(synced)
    }
}
