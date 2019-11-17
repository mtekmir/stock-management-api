package com.merit.modules.products

import com.merit.modules.excel.ExcelProductRow
import slick.jdbc.PostgresProfile
import scala.concurrent.Future
import slick.dbio.DBIO
import com.merit.modules.brands.{BrandID, BrandRepo}
import scala.concurrent.ExecutionContext

trait ProductService {
  def batchInsertExcelRows(rows: Vector[ExcelProductRow]): Future[Seq[ProductID]]
  def get(barcode: String): Future[Option[ProductDTO]]
}

object ProductService {
  def apply(
    db: PostgresProfile.backend.Database,
    brandRepo: BrandRepo[DBIO],
    productRepo: ProductRepo[DBIO]
  )(implicit ec: ExecutionContext) = new ProductService {

    def batchInsertExcelRows(
      productRows: Vector[ExcelProductRow]
    ): Future[Seq[ProductID]] = {
      val brandsMap = db
        .run(brandRepo.getAll())
        .map(_.foldLeft(Map[String, BrandID]())((m, b) => m + (b.name -> b.id)))

      for {
        bMap <- brandsMap
        products <- Future.successful(productRows.map {
          case ExcelProductRow(barcode, variation, sku, name, price, qty, brand) =>
            ProductRow(barcode, sku, name, price, qty, variation, bMap(brand))
        })
        ids <- db.run(productRepo.batchInsert(products.toSeq))
      } yield (ids)
    }

    def get(barcode: String): Future[Option[ProductDTO]] = 
      db.run(productRepo.get(barcode))
  }
}
