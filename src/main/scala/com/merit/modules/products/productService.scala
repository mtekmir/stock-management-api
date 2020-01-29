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
import cats.data.OptionT
import cats.instances.future._
import cats.data.EitherT
import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging

trait ProductService {
  def batchInsertExcelRows(rows: Seq[ExcelProductRow]): Future[Seq[ProductRow]]
  def getProduct(barcode: String): Future[Option[ProductDTO]]
  def getProducts(
    page: Int,
    rowsPerPage: Int,
    filters: ProductFilters = ProductFilters()
  ): Future[PaginatedProductsResponse]
  def findAll(barcodes: Seq[String]): Future[Seq[ProductDTO]]
  def batchAddQuantity(products: Seq[(String, Int)]): Future[Seq[Int]]
  def searchProducts(query: String): Future[Seq[ProductDTO]]
  def createProduct(p: CreateProductRequest): Future[Either[String, ProductDTO]]
  def editProduct(
    id: ProductID,
    fields: EditProductRequest
  ): Future[Either[String, ProductDTO]]
}

object ProductService {
  def apply(
    db: Database,
    brandRepo: BrandRepo[DBIO],
    productRepo: ProductRepo[DBIO],
    categoryRepo: CategoryRepo[DBIO]
  )(implicit ec: ExecutionContext) = new ProductService with LazyLogging {
    private def insertBrandIfNotExists(brandName: String): DBIO[BrandRow] =
      brandRepo
        .get(brandName)
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
    ): Future[Seq[ProductRow]] = {
      logger.info(s"Batch inserting ${rows.length} products")
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
    }

    def getProduct(barcode: String): Future[Option[ProductDTO]] =
      db.run(productRepo.get(barcode))

    def getProducts(
      page: Int,
      rowsPerPage: Int,
      filters: ProductFilters
    ): Future[PaginatedProductsResponse] =
      for {
        products <- db.run(productRepo.getAll(page, rowsPerPage, filters))
        count    <- db.run(productRepo.count(filters))
      } yield PaginatedProductsResponse(count, products)

    def findAll(barcodes: Seq[String]): Future[Seq[ProductDTO]] =
      db.run(productRepo.findAll(barcodes))

    def batchAddQuantity(products: Seq[(String, Int)]): Future[Seq[Int]] =
      db.run(DBIO.sequence(products.map(p => productRepo.addQuantity(p._1, p._2))))

    def searchProducts(query: String): Future[Seq[ProductDTO]] =
      db.run(productRepo.search(query))

    def createProduct(p: CreateProductRequest): Future[Either[String, ProductDTO]] = {
      logger.info(s"Creating a product: Barcode ${p.barcode}, sku ${p.sku}, name ${p.name}")
      EitherT
        .liftF(db.run(productRepo.get(p.barcode)))
        .flatMapF {
          case None => db.run(productRepo.create(p.toRow)).map(_.asRight)
          case Some(_) =>
            logger.warn(s"Duplicate barcode value provided {${p.barcode}}")
            Future.successful(s"Barcode ${p.barcode} already exists".asLeft)
        }
        .value
    }

    def editProduct(
      id: ProductID,
      fields: EditProductRequest
    ): Future[Either[String, ProductDTO]] = {
      logger.info(s"Editing product with id $id")
      def duplicate =
        OptionT
          .fromOption(fields.barcode)
          .flatMap(b => OptionT.liftF(db.run(productRepo.get(b).map(_.isDefined))))
          .value

      def edit =
        (for {
          product <- OptionT(db.run(productRepo.getRow(id)))
          _       <- OptionT.liftF(db.run(productRepo.edit(product, fields)))
          dto     <- OptionT(db.run(productRepo.get(id)))
        } yield dto).value

      (EitherT
        .liftF(duplicate)
        .flatMapF {
          case Some(true) =>
            logger.warn(s"Duplicate barcode value provided {${fields.barcode}}")
            Future.successful("Barcode already exists".asLeft)
          case _ =>
            edit.map(Either.fromOption(_, s"Product with an id of ${id.value} not found"))
        })
        .value
    }
  }
}
