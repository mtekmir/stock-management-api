package com.merit.modules.products

import com.merit.modules.excel.ExcelProductRow
import slick.jdbc.PostgresProfile
import scala.concurrent.Future
import slick.dbio.DBIO
import com.merit.modules.brands.{BrandID, BrandRepo}
import scala.concurrent.ExecutionContext
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.categories.CategoryID
import slick.jdbc.PostgresProfile.api._
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import slick.jdbc.JdbcBackend.Database

trait ProductService {
  def batchInsertExcelRows(rows: Seq[ExcelProductRow]): Future[Seq[ProductRow]]
  def get(barcode: String): Future[Option[ProductDTO]]
  def getProducts(page: Int, rowsPerPage: Int): Future[PaginatedProductsResponse]
  def findAll(barcodes: Seq[String]): Future[Seq[ProductDTO]]
  def batchAddQuantity(products: Seq[(String, Int)]): Future[Seq[Int]]
  def searchProducts(query: String): Future[Seq[ProductDTO]]
}

object ProductService {
  def apply(
    db: Database,
    brandRepo: BrandRepo[DBIO],
    productRepo: ProductRepo[DBIO],
    categoryRepo: CategoryRepo[DBIO]
  )(implicit ec: ExecutionContext) = new ProductService {
    private def insertBrandIfNotExists(brandName: String): DBIO[BrandRow] =
      brandRepo
        .getByName(brandName)
        .flatMap {
          case Some(b) => DBIO.successful(b)
          case None    => brandRepo.insert(BrandRow(brandName))
        }
        .transactionally

    private def insertCategoryIfNotExists(categoryName: String): DBIO[CategoryRow] =
      categoryRepo
        .getByName(categoryName)
        .flatMap {
          case Some(c) => DBIO.successful(c)
          case None    => categoryRepo.insert(CategoryRow(categoryName))
        }
        .transactionally

    def batchInsertExcelRows(
      rows: Seq[ExcelProductRow]
    ): Future[Seq[ProductRow]] =
      db.run(for {
        brands <- DBIO.sequence(
          rows.map(_.brand).flatten.distinct.map(b => insertBrandIfNotExists(b))
        )
        categories <- DBIO.sequence(
          rows.map(_.category).flatten.distinct.map(c => insertCategoryIfNotExists(c))
        )
        productRows <- DBIO.successful(
          rows.map(
            r =>
              ExcelProductRow.toProductRow(
                r,
                r.brand.flatMap(b => brands.find(_.name == b).map(_.id)),
                r.category.flatMap(c => categories.find(_.name == c).map(_.id))
              )
          )
        )
        products <- productRepo.batchInsert(productRows)
      } yield products)

    def get(barcode: String): Future[Option[ProductDTO]] =
      db.run(productRepo.get(barcode))

    def getProducts(page: Int, rowsPerPage: Int): Future[PaginatedProductsResponse] =
      for {
        products <- db.run(productRepo.getAll(page, rowsPerPage))
        count    <- db.run(productRepo.count)
      } yield PaginatedProductsResponse(count, products)

    def findAll(barcodes: Seq[String]): Future[Seq[ProductDTO]] =
      db.run(productRepo.findAll(barcodes))

    def batchAddQuantity(products: Seq[(String, Int)]): Future[Seq[Int]] =
      db.run(DBIO.sequence(products.map(p => productRepo.addQuantity(p._1, p._2))))
    
    def searchProducts(query: String): Future[Seq[ProductDTO]] = 
      db.run(productRepo.search(query))
  }
}
