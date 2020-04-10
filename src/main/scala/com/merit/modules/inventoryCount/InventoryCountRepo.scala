package com.merit.modules.inventoryCount

import db.Schema
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import com.merit.modules.products.ProductID
import scala.concurrent.ExecutionContext
import com.merit.modules.inventoryCount.InventoryCountProductStatus._

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
  def cancelInventoryCount(batchId: InventoryCountBatchID): DbTask[Int]
  def completeInventoryCount(batchId: InventoryCountBatchID): DbTask[Int]
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
          .map(_.counted)
          .update(Some(count))

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

      // def filterOnStatus(q: TableQuery[InventoryCountProductsTable], s: InventoryCountProductStatus) =
      //   q.filter(p => s match {
      //     case All => true
      //     case Counted => p.counted.isDefined
      //     case UnCounted => p.counted.isEmpty
      //   })

      def getBatchProducts(
        batchId: InventoryCountBatchID,
        status: InventoryCountProductStatus,
        page: Int,
        rowsPerPage: Int
      ): DBIO[Seq[InventoryCountProductDTO]] =
        inventoryCountProducts
          .filter(
            _.batchId === batchId
          )
          // .filter(p =>
          //   status match {
          //     case InventoryCountProductStatus.Counted   => p.counted.isDefined
          //     case InventoryCountProductStatus.UnCounted => p.counted.isEmpty
          //     case _      => true
          //   }
          // )
          .drop((page - 1) * rowsPerPage)
          .take(rowsPerPage)
          .join(products)
          .on(_.productId === _.id)
          .result
          .map {
            _.map {
              case (batchProductRow, productRow) =>
                InventoryCountProductDTO.fromRow(batchProductRow, productRow)
            }
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
    }
}
