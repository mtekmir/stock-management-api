package com.merit.modules.sales

import com.merit.modules.products.{ProductRow, SoldProductRow}
import com.merit.modules.brands.BrandRow
import db.Schema
import com.merit.modules.categories.CategoryRow

trait SaleRepo[DbTask[_]] {
  def add(sale: SaleRow): DbTask[SaleRow]
  def addProductsToSale(
    products: Seq[SoldProductRow]
  ): DbTask[Seq[SoldProductRow]]
  def get(
    id: SaleID
  ): DbTask[Seq[(SaleRow, ProductRow, Int, Option[BrandRow], Option[CategoryRow])]]
}

object SaleRepo {
  def apply(schema: Schema) = new SaleRepo[slick.dbio.DBIO] {
    import schema._
    import schema.profile.api._

    def add(sale: SaleRow): DBIO[SaleRow] =
      sales returning sales += sale

    def addProductsToSale(
      products: Seq[SoldProductRow]
    ): DBIO[Seq[SoldProductRow]] =
      soldProducts returning soldProducts ++= products

    def get(
      id: SaleID
    ): DBIO[Seq[(SaleRow, ProductRow, Int, Option[BrandRow], Option[CategoryRow])]] =
      sales
        .filter(_.id === id)
        .join(soldProducts)
        .on(_.id === _.saleId)
        .join(products)
        .on {
          case ((sales, soldProducts), products) => soldProducts.productId === products.id
        }
        .joinLeft(brands)
        .on {
          case (((sales, soldProducts), products), brands) =>
            products.brandId === brands.id
        }
        .joinLeft(categories)
        .on {
          case ((((sales, soldProducts), products), brands), categories) =>
            products.categoryId === categories.id
        }
        .map {
          case ((((s, sp), p), b), c) => (s, p, sp.qty, b, c)
        }
        .result
  }
}
