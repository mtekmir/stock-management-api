package com.merit.modules.products

import db.Schema
import scala.concurrent.ExecutionContext
import slick.sql.SqlAction
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow

trait ProductRepo[DbTask[_]] {
  def get(barcode: String): DbTask[Option[ProductDTO]]
  def getRow(barcode: String): DbTask[Option[ProductRow]]
  def getAll: DbTask[Seq[ProductDTO]]
  def findAll(barcodes: Seq[String]): DbTask[Seq[ProductDTO]]
  def insert(product: ProductRow): DbTask[ProductRow]
  def batchInsert(products: Seq[ProductRow]): DbTask[Seq[ProductRow]]
  def deductQuantity(barcode: String, qty: Int): DbTask[Int]
  def addQuantity(barcode: String, qty: Int): DbTask[Int]
}

object ProductRepo {
  def apply(schema: Schema)(implicit ec: ExecutionContext) =
    new ProductRepo[slick.dbio.DBIO] {
      import schema._
      import profile.api._

      private def withBrandAndCategory(q: Query[schema.ProductTable, ProductRow, Seq]) =
        q.joinLeft(brands)
          .on(_.brandId === _.id)
          .joinLeft(categories)
          .on {
            case ((products, brands), category) => products.categoryId === category.id
          }

      def get(barcode: String): DBIO[Option[ProductDTO]] =
        withBrandAndCategory(products.filter(_.barcode === barcode)).result.headOption.map {
          _.map {
            case ((pRow, bRow), cRow) => ProductDTO.fromRow(pRow, bRow, cRow)
          }
        }
      
      def getRow(barcode: String): DBIO[Option[ProductRow]] = 
        products.filter(_.barcode === barcode).result.headOption

      def getAll: DBIO[Seq[ProductDTO]] = 
        withBrandAndCategory(products).result.map {
          _.map {
            case ((pRow, bRow), cRow) => ProductDTO.fromRow(pRow, bRow, cRow)
          }
        }

      def findAll(barcodes: Seq[String]): DBIO[Seq[ProductDTO]] =
        withBrandAndCategory(products.filter(_.barcode inSet barcodes)).result.map {
          _.map {
            case ((pRow, bRow), cRow) => ProductDTO.fromRow(pRow, bRow, cRow)
          }
        }

      def insert(product: ProductRow): DBIO[ProductRow] =
        products returning products += product

      def batchInsert(ps: Seq[ProductRow]): DBIO[Seq[ProductRow]] =
        products returning products ++= ps

      def deductQuantity(barcode: String, qty: Int): DBIO[Int] =
        sqlu"update products set qty = qty - $qty where barcode=$barcode"

      def addQuantity(barcode: String, qty: Int): DBIO[Int] =
        sqlu"update products set qty = qty + $qty where barcode=$barcode"

    }
}
