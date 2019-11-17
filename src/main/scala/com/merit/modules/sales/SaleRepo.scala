package com.merit.modules.sales

import com.merit.modules.products.{ProductRow, SoldProductRow}
import com.merit.modules.brands.BrandRow
import db.Schema

trait SaleRepo[DbTask[_]] {
  def add(sale: SaleRow): DbTask[SaleRow]
  def addProductsToSale(
    products: Seq[SoldProductRow]
  ): DbTask[Seq[SoldProductRow]]
  def get(id: SaleID): DbTask[Seq[(SaleRow, ProductRow, Int, BrandRow)]]
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

    def get(id: SaleID): DBIO[Seq[(SaleRow, ProductRow, Int, BrandRow)]] =
      (for {
        sale           <- sales.filter(_.id === id)
        sp <- soldProducts.filter(_.saleId === sale.id)
        spp   <- products.filter(_.id === sp.productId)
        brands         <- brands.filter(_.id === spp.brandId)
      } yield (sale, spp, sp.qty, brands)).result

  }
}
