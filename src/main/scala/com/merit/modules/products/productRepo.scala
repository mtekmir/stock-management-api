package com.merit.modules.products

import db.Schema
import scala.concurrent.ExecutionContext

trait ProductRepo[DbTask[_]] {
  def get(barcode: String): DbTask[Option[ProductDTO]]
  def findAll(barcodes: Vector[String]): DbTask[Seq[ProductDTO]]
  def add(product: ProductRow): DbTask[ProductID]
  def batchInsert(products: Seq[ProductRow]): DbTask[Seq[ProductID]]
  def deductQuantity(barcode: String, qty: Int): DbTask[Int]
}

object ProductRepo {
  def apply(schema: Schema)(implicit ec: ExecutionContext) =
    new ProductRepo[slick.dbio.DBIO] {
      import schema._
      import profile.api._

      def get(barcode: String): DBIO[Option[ProductDTO]] =
        (for {
          product <- products.filter(_.barcode === barcode)
          brand   <- brands.filter(_.id === product.brandId)
        } yield (product, brand)).result.headOption.map {
          _.map {
            case (p, b) => ProductDTO.fromRow(p, b)
          }
        }

      def findAll(barcodes: Vector[String]): DBIO[Seq[ProductDTO]] =
        (for {
          product <- products.filter(_.barcode inSet barcodes)
          brand <- brands.filter(_.id === product.brandId)
        } yield (product, brand)).result.map{
          _.map(r => ProductDTO.fromRow(r._1,r._2))
        }

      def add(product: ProductRow): DBIO[ProductID] =
        products returning products.map(_.id) += product

      def batchInsert(ps: Seq[ProductRow]): DBIO[Seq[ProductID]] =
        products returning products.map(_.id) ++= ps

      def deductQuantity(barcode: String, qty: Int): DBIO[Int] =
        sqlu"update products set qty = qty - $qty where barcode=$barcode"

    }
}
