package com.merit.modules.products

import db.Schema
import scala.concurrent.ExecutionContext
import slick.sql.SqlAction
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import cats.implicits._
import cats.Functor
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID

trait ProductRepo[DbTask[_]] {
  def count(filters: ProductFilters): DbTask[Int]
  def get(barcode: String): DbTask[Option[ProductDTO]]
  def get(id: ProductID): DbTask[Option[ProductDTO]]
  def getRow(barcode: String): DbTask[Option[ProductRow]]
  def getRow(id: ProductID): DbTask[Option[ProductRow]]
  def getAll(page: Int, rowsPerPage: Int, filters: ProductFilters): DbTask[Seq[ProductDTO]]
  def getAll(filters: ProductFilters): DbTask[Seq[ProductDTO]]
  def findAll(barcodes: Seq[String]): DbTask[Seq[ProductDTO]]
  def insert(product: ProductRow): DbTask[ProductRow]
  def batchInsert(products: Seq[ProductRow]): DbTask[Seq[ProductRow]]
  def deductQuantity(barcode: String, qty: Int): DbTask[Int]
  def addQuantity(barcode: String, qty: Int): DbTask[Int]
  def search(query: String): DbTask[Seq[ProductDTO]]
  def create(product: ProductRow): DbTask[ProductDTO]
  def edit(product: ProductRow, fields: EditProductRequest): DbTask[Int]
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

      private type ProductQuery = Query[ProductTable, ProductRow, Seq]

      implicit class FilterQ1(
        query: ProductQuery
      ) {
        def filterQueryString(
          filter: Option[String]
        ): ProductQuery =
          filter.foldLeft(query) {
            case (q, f) => {
              val queryString = s"$f%"
              q.filter(
                p =>
                  (p.name.toLowerCase like queryString.toLowerCase) || (p.barcode like queryString) || (p.sku like queryString)
              )
            }
          }

        def filterOption[A: BaseColumnType](
          filter: Option[A],
          tableToColumn: ProductTable => Rep[Option[A]]
        ): ProductQuery =
          filter.foldLeft(query) {
            case (q, f) => q.filter(t => tableToColumn(t) === f)
          }

        def withBrandAndCategory =
          query
            .joinLeft(brands)
            .on(_.brandId === _.id)
            .joinLeft(categories)
            .on {
              case ((products, brands), category) => products.categoryId === category.id
            }
      }

      def get(barcode: String): DBIO[Option[ProductDTO]] =
        products
          .filter(_.barcode === barcode)
          .withBrandAndCategory
          .result
          .headOption
          .toProductDTO

      def get(id: ProductID): DBIO[Option[ProductDTO]] =
        products
          .filter(_.id === id)
          .withBrandAndCategory
          .result
          .headOption
          .toProductDTO

      def getRow(barcode: String): DBIO[Option[ProductRow]] =
        products.filter(_.barcode === barcode).result.headOption

      def getRow(id: ProductID): DBIO[Option[ProductRow]] =
        products.filter(_.id === id).result.headOption

      private def getAllBase(filters: ProductFilters): ProductQuery =
        products
          .filterOption(filters.brandId, _.brandId)
          .filterOption(filters.categoryId, _.categoryId)
          .filterQueryString(filters.query)

      def count(filters: ProductFilters): DBIO[Int] = getAllBase(filters).length.result

      def getAll(filters: ProductFilters): DBIO[Seq[ProductDTO]] =
        getAllBase(filters).withBrandAndCategory.result
          .map(_.toList)
          .toProductDTO

      def getAll(
        page: Int,
        rowsPerPage: Int,
        filters: ProductFilters = ProductFilters()
      ): DBIO[Seq[ProductDTO]] =
        getAllBase(filters)
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
          .filter(
            p =>
              (p.name.toLowerCase like query.toLowerCase) || (p.barcode like query) || (p.sku like query)
          )
          .withBrandAndCategory
          .take(20)
          .result
          .map(_.toList)
          .toProductDTO
      }

      def create(product: ProductRow): DBIO[ProductDTO] =
        for {
          row         <- products returning products += product
          brandRow    <- brands.filter(_.id === row.brandId).result.headOption
          categoryRow <- categories.filter(_.id === row.categoryId).result.headOption
        } yield ProductDTO.fromRow(row, brandRow, categoryRow)

      def edit(product: ProductRow, fields: EditProductRequest): DBIO[Int] = {
        def isNum(v: String): Boolean = v.forall(_.isDigit)
        def diff(product: ProductRow, fields: EditProductRequest) =
          fields.getClass.getDeclaredFields
            .map(_.getName)
            .zip(fields.productIterator)
            .foldLeft(product) {
              case (product, (field, value)) =>
                (field, value) match {
                  case ("barcode", Some(v: String)) => product.copy(barcode = v)
                  case ("sku", Some(v: String))     => product.copy(sku = v)
                  case ("name", Some(v: String))    => product.copy(name = v)
                  case ("price", Some(v: String))   => product.copy(price = Currency.from(v))
                  case ("discountPrice", Some(v: String)) =>
                    product.copy(discountPrice = Currency.from(v))
                  case ("qty", Some(v: String)) if isNum(v) =>
                    product.copy(qty = v.toInt)
                  case ("variation", Some(v: String)) => product.copy(variation = Some(v))
                  case ("taxRate", Some(v: String)) if isNum(v) =>
                    product.copy(taxRate = Some(v.toInt))
                  case ("brandId", Some(v: String)) if isNum(v) =>
                    product.copy(brandId = Some(BrandID(v.toLong)))
                  case ("categoryId", Some(v: String)) if isNum(v) =>
                    product.copy(categoryId = Some(CategoryID(v.toLong)))
                  case (_, _) => product
                }
            }

        products.filter(_.id === product.id).update(diff(product, fields))
      }

    }
}
