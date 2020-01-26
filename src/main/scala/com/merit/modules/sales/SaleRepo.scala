package com.merit.modules.sales

import com.merit.modules.products.{ProductRow, SoldProductRow}
import com.merit.modules.brands.BrandRow
import db.Schema
import com.merit.modules.categories.CategoryRow
import com.merit.modules.products.ProductID
import org.joda.time.DateTime

trait SaleRepo[DbTask[_]] {
  def insert(sale: SaleRow): DbTask[SaleRow]
  def addProductsToSale(
    products: Seq[SoldProductRow]
  ): DbTask[Seq[SoldProductRow]]
  def get(
    id: SaleID
  ): DbTask[Seq[(SaleRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow])]]
  def getAll(
    page: Int,
    rowsPerPage: Int,
    filters: SaleFilters
  ): DbTask[Seq[(SaleRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow])]]
  def count(filters: SaleFilters): DbTask[Int]
  def syncSoldProduct(saleId: SaleID, productId: ProductID, synced: Boolean): DbTask[Int]
}

object SaleRepo {
  def apply(schema: Schema) = new SaleRepo[slick.dbio.DBIO] {
    import schema._
    import schema.CustomColumnTypes._
    import schema.profile.api._

    private type SaleQuery = Query[SaleTable, SaleRow, Seq]
    implicit class FilterQ1(
      query: SaleQuery
    ) {
      def filterByDates(
        filters: SaleFilters,
      ): SaleQuery = {
        import filters._
        (startDate, endDate) match {
          case (Some(sd), Some(ed)) =>
            query.filter(t => t.createdAt >= sd && t.createdAt <= ed)
          case (Some(sd), None) =>
            query.filter(_.createdAt >= sd)
          case (None, Some(ed)) => query.filter(_.createdAt <= ed)
          case (None, None)     => query
        }
      }

      def withProducts: Query[
        (
          SaleTable,
          ProductTable,
          Rep[Int],
          Rep[Boolean],
          Rep[Option[BrandTable]],
          Rep[Option[schema.CategoryTable]]
        ),
        (SaleRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow]),
        Seq
      ] =
        query
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
            case ((((s, sp), p), b), c) => (s, p, sp.qty, sp.synced, b, c)
          }
    }

    def insert(sale: SaleRow): DBIO[SaleRow] =
      sales returning sales += sale

    def addProductsToSale(
      products: Seq[SoldProductRow]
    ): DBIO[Seq[SoldProductRow]] =
      soldProducts returning soldProducts ++= products

    def get(
      id: SaleID
    ): DBIO[Seq[(SaleRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow])]] =
      sales.filter(_.id === id).withProducts.result

    def getAll(
      page: Int,
      rowsPerPage: Int,
      filters: SaleFilters
    ): DBIO[Seq[(SaleRow, ProductRow, Int, Boolean, Option[BrandRow], Option[CategoryRow])]] =
      sales
        .filterByDates(filters)
        .drop((page - 1) * rowsPerPage)
        .take(rowsPerPage)
        .withProducts
        .sortBy(_._1.createdAt.desc)
        .result

    def count(filters: SaleFilters): DBIO[Int] =
      sales.filterByDates(filters).length.result

    def syncSoldProduct(saleId: SaleID, productId: ProductID, synced: Boolean): DBIO[Int] =
      soldProducts
        .filter(p => p.productId === productId && p.saleId === saleId)
        .map(_.synced)
        .update(synced)
  }
}
