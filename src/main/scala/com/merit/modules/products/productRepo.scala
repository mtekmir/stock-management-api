package com.merit.modules.products

import db.Schema
import scala.concurrent.ExecutionContext
import slick.sql.SqlAction
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import cats.implicits._
import cats.Functor

trait ProductRepo[DbTask[_]] {
  def count: DbTask[Int]
  def get(barcode: String): DbTask[Option[ProductDTO]]
  def getRow(barcode: String): DbTask[Option[ProductRow]]
  def getAll(page: Int, rowsPerPage: Int): DbTask[Seq[ProductDTO]]
  def findAll(barcodes: Seq[String]): DbTask[Seq[ProductDTO]]
  def insert(product: ProductRow): DbTask[ProductRow]
  def batchInsert(products: Seq[ProductRow]): DbTask[Seq[ProductRow]]
  def deductQuantity(barcode: String, qty: Int): DbTask[Int]
  def addQuantity(barcode: String, qty: Int): DbTask[Int]
  def search(query: String): DbTask[Seq[ProductDTO]]
}

object ProductRepo {
  def apply(schema: Schema)(implicit ec: ExecutionContext) =
    new ProductRepo[slick.dbio.DBIO] {
      import schema._
      import profile.api._

      implicit private class ProductQ1[M[_]: Functor](
        val q: DBIO[M[((ProductRow, Option[BrandRow]), Option[CategoryRow])]]
      ) {
        def toProductDTO = q.map {
          _.map {
            case ((pRow, bRow), cRow) => ProductDTO.fromRow(pRow, bRow, cRow)
          }
        }
      }

      implicit private class ProductQ2(val q: Query[ProductTable, ProductRow, Seq]) {
        def withBrandAndCategory =
          q.joinLeft(brands)
            .on(_.brandId === _.id)
            .joinLeft(categories)
            .on {
              case ((products, brands), category) => products.categoryId === category.id
            }
      }

      def count: DBIO[Int] = products.length.result

      def get(barcode: String): DBIO[Option[ProductDTO]] =
        products
          .filter(_.barcode === barcode)
          .withBrandAndCategory
          .result
          .headOption
          .toProductDTO

      def getRow(barcode: String): DBIO[Option[ProductRow]] =
        products.filter(_.barcode === barcode).result.headOption

      def getAll(page: Int, rowsPerPage: Int): DBIO[Seq[ProductDTO]] =
        products
          .drop((page - 1) * rowsPerPage)
          .take(rowsPerPage)
          .withBrandAndCategory
          .sortBy(_._1._1.id.desc)
          .result
          .map(_.toList)
          .toProductDTO

      def findAll(barcodes: Seq[String]): DBIO[Seq[ProductDTO]] =
        products
          .filter(_.barcode inSet barcodes)
          .withBrandAndCategory
          .result
          .map(_.toList)
          .toProductDTO

      def insert(product: ProductRow): DBIO[ProductRow] =
        products returning products += product

      def batchInsert(ps: Seq[ProductRow]): DBIO[Seq[ProductRow]] =
        products returning products ++= ps

      def deductQuantity(barcode: String, qty: Int): DBIO[Int] =
        sqlu"update products set qty = qty - $qty where barcode=$barcode"

      def addQuantity(barcode: String, qty: Int): DBIO[Int] =
        sqlu"update products set qty = qty + $qty where barcode=$barcode"

      def search(q: String): DBIO[Seq[ProductDTO]] = {
        val query = s"$q%"
        products
          .filter(p => (p.name like query) || (p.barcode like query) || (p.sku like query))
          .withBrandAndCategory
          .take(20)
          .result
          .map(_.toList)
          .toProductDTO
      }

    }
}
