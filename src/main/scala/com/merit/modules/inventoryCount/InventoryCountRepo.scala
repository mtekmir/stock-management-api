package com.merit.modules.inventoryCount

import db.Schema
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import com.merit.modules.products.ProductID
import scala.concurrent.ExecutionContext
import com.merit.modules.inventoryCount.InventoryCountProductStatus._
import org.joda.time.DateTime

trait InventoryCountRepo[DbTask[_]] {
  def count(
    status: InventoryCountStatus
  ): DbTask[Int]
  def productCount(batchId: InventoryCountBatchID, counted: Boolean): DbTask[Int]
  def insertBatch(batch: InventoryCountBatchRow): DbTask[InventoryCountBatchRow]
  def addProductsToBatch(
    products: Seq[InventoryCountProductRow]
  ): DbTask[Seq[InventoryCountProductRow]]
  def get(
    id: InventoryCountBatchID
  ): DbTask[Option[
    (
      InventoryCountBatchRow,
      Option[CategoryRow],
      Option[BrandRow]
    )
  ]]
  def getAll(
    page: Int,
    rowsPerPage: Int,
    status: InventoryCountStatus
  ): DbTask[Seq[
    (
      InventoryCountBatchRow,
      Option[CategoryRow],
      Option[BrandRow]
    )
  ]]
  def countProduct(
    productId: InventoryCountProductID,
    count: Int
  ): DbTask[Int]
  def getProduct(
    productId: InventoryCountProductID
  ): DbTask[Option[InventoryCountProductDTO]]
  def getBatchProducts(
    batchId: InventoryCountBatchID,
    status: InventoryCountProductStatus,
    page: Int,
    rowsPerPage: Int
  ): DbTask[Seq[InventoryCountProductDTO]]
  def searchBatchProducts(
    batchId: InventoryCountBatchID,
    query: String
  ): DbTask[Seq[InventoryCountProductDTO]]
  def cancelInventoryCount(batchId: InventoryCountBatchID): DbTask[Int]
  def completeInventoryCount(batchId: InventoryCountBatchID): DbTask[Int]
  def getAllProductsOfBatch(
    batchId: InventoryCountBatchID
  ): DbTask[Seq[InventoryCountProductDTO]]
  def deleteInventoryCountProduct(
    id: InventoryCountProductID
  ): DbTask[Int]
  def deleteAllInventoryCountProducts(
    batchId: InventoryCountBatchID
  ): DbTask[Int]
  def deleteBatch(id: InventoryCountBatchID): DbTask[Int]
}

object InventoryCountRepo {
  def apply(schema: Schema)(implicit ec: ExecutionContext) =
    new InventoryCountRepo[slick.dbio.DBIO] {
      import schema._
      import schema.profile.api._

      def count(
        status: InventoryCountStatus
      ): DBIO[Int] =
        inventoryCountBatches.filter(_.status === status).length.result

      def productCount(batchId: InventoryCountBatchID, counted: Boolean): DBIO[Int] =
        inventoryCountProducts
          .filter(p => p.batchId === batchId && p.counted.isDefined === counted)
          .length
          .result

      def insertBatch(batch: InventoryCountBatchRow): DBIO[InventoryCountBatchRow] =
        inventoryCountBatches returning inventoryCountBatches += batch

      def addProductsToBatch(
        products: Seq[InventoryCountProductRow]
      ): DBIO[Seq[InventoryCountProductRow]] =
        inventoryCountProducts returning inventoryCountProducts ++= products

      implicit class Q1(
        val q: Query[schema.InventoryCountBatchTable, InventoryCountBatchRow, Seq]
      ) {
        def withCategoryAndBrand =
          q.joinLeft(categories)
            .on {
              case (batch, categories) => batch.categoryId === categories.id
            }
            .joinLeft(brands)
            .on {
              case ((batch, categories), brands) => batch.brandId === brands.id
            }
            .map {
              case ((batch, categories), brands) => (batch, categories, brands)
            }
      }

      def get(
        id: InventoryCountBatchID
      ) =
        inventoryCountBatches
          .filter(_.id === id)
          .withCategoryAndBrand
          .result
          .headOption

      def getAll(page: Int, rowsPerPage: Int, status: InventoryCountStatus): DBIO[Seq[
        (
          InventoryCountBatchRow,
          Option[CategoryRow],
          Option[BrandRow]
        )
      ]] =
        inventoryCountBatches
          .filter(_.status === status)
          .sortBy(_.started.desc)
          .drop((page - 1) * rowsPerPage)
          .take(rowsPerPage)
          .withCategoryAndBrand
          .sortBy(_._1.started.desc)
          .result

      def countProduct(
        productId: InventoryCountProductID,
        count: Int
      ): DBIO[Int] =
        inventoryCountProducts
          .filter(_.id === productId)
          .map(p => (p.counted, p.updatedAt))
          .update((Some(count), DateTime.now()))

      def getProduct(
        productId: InventoryCountProductID
      ): DBIO[Option[InventoryCountProductDTO]] =
        inventoryCountProducts
          .filter(_.id === productId)
          .join(products)
          .on(_.productId === _.id)
          .result
          .headOption
          .map {
            _.map {
              case (batchProductRow, productRow) =>
                InventoryCountProductDTO.fromRow(batchProductRow, productRow)
            }
          }

      def getBatchProducts(
        batchId: InventoryCountBatchID,
        status: InventoryCountProductStatus,
        page: Int,
        rowsPerPage: Int
      ): DBIO[Seq[InventoryCountProductDTO]] =
        inventoryCountProducts
          .filter(_.batchId === batchId)
          .filter(
            p =>
              status match {
                case InventoryCountProductStatus.Counted   => p.counted.isDefined
                case InventoryCountProductStatus.UnCounted => p.counted.isEmpty
                case _                                     => p.counted.isEmpty || p.counted.isDefined
              }
          )
          .sortBy(_.updatedAt.desc)
          .drop((page - 1) * rowsPerPage)
          .take(rowsPerPage)
          .join(products)
          .on(_.productId === _.id)
          .sortBy(_._1.updatedAt.desc)
          .result
          .toInventoryCountProductDTOS

      def searchBatchProducts(
        batchId: InventoryCountBatchID,
        query: String
      ): DBIO[Seq[InventoryCountProductDTO]] = {
        val q = s"${query.toLowerCase()}%"
        inventoryCountProducts
          .join(products)
          .on(_.productId === _.id)
          .filter(_._1.batchId === batchId)
          .filter {
            case (_, p) =>
              (p.name.toLowerCase like q) || (p.barcode like q) || (p.sku.toLowerCase like q)
          }
          .result
          .toInventoryCountProductDTOS
      }

      def cancelInventoryCount(batchId: InventoryCountBatchID): DBIO[Int] =
        inventoryCountBatches
          .filter(_.id === batchId)
          .map(_.status)
          .update(InventoryCountStatus.Cancelled)

      def completeInventoryCount(batchId: InventoryCountBatchID): DBIO[Int] =
        inventoryCountBatches
          .filter(_.id === batchId)
          .map(_.status)
          .update(InventoryCountStatus.Completed)

      def getAllProductsOfBatch(
        batchId: InventoryCountBatchID
      ): DBIO[Seq[InventoryCountProductDTO]] =
        inventoryCountProducts
          .filter(_.batchId === batchId)
          .join(products)
          .on(_.productId === _.id)
          .result
          .toInventoryCountProductDTOS

      def deleteInventoryCountProduct(id: InventoryCountProductID): DBIO[Int] =
        inventoryCountProducts.filter(_.id === id).delete

      def deleteAllInventoryCountProducts(batchId: InventoryCountBatchID): DBIO[Int] =
        inventoryCountProducts.filter(_.batchId === batchId).delete

      def deleteBatch(id: InventoryCountBatchID): DBIO[Int] =
        inventoryCountBatches.filter(_.id === id).delete
    }
}
