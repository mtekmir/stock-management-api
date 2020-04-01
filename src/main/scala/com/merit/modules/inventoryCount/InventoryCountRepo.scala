package com.merit.modules.inventoryCount

import db.Schema
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import com.merit.modules.products.ProductID
import scala.concurrent.ExecutionContext

trait InventoryCountRepo[DbTask[_]] {
  def count: DbTask[Int]
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
    rowsPerPage: Int
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
  ): DbTask[Option[InventoryCountDTOProduct]]
  def cancelInventoryCount(batchId: InventoryCountBatchID): DbTask[Int]
  def completeInventoryCount(batchId: InventoryCountBatchID): DbTask[Int]
}

object InventoryCountRepo {
  def apply(schema: Schema)(implicit ec: ExecutionContext) =
    new InventoryCountRepo[slick.dbio.DBIO] {
      import schema._
      import schema.profile.api._

      def count: DBIO[Int] =
        inventoryCountBatches.length.result

      def insertBatch(batch: InventoryCountBatchRow): DBIO[InventoryCountBatchRow] =
        inventoryCountBatches returning inventoryCountBatches += batch

      def addProductsToBatch(
        products: Seq[InventoryCountProductRow]
      ): DBIO[Seq[InventoryCountProductRow]] =
        inventoryCountProducts returning inventoryCountProducts ++= products

      implicit class Q1(
        val q: Query[schema.InventoryCountBatchTable, InventoryCountBatchRow, Seq]
      ) {
        def withBrandCategoryAndProducts =
          q.joinLeft(categories)
            .on {
              case (batch, categories) => batch.categoryId === categories.id
            }
            .joinLeft(brands)
            .on {
              case ((batch, categories), brands) => batch.brandId === brands.id
            }
            .join(inventoryCountProducts)
            .on {
              case (((batch, _), _), batchProducts) => batch.id === batchProducts.batchId
            }
            .join(products)
            .on {
              case ((((_, _), _), batchProducts), products) =>
                batchProducts.productId === products.id
            }
            .map {
              case ((((batch, category), brand), batchProducts), products) =>
                (batch, category, brand, batchProducts, products)
            }

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

      def getAll(page: Int, rowsPerPage: Int): DBIO[Seq[
        (
          InventoryCountBatchRow,
          Option[CategoryRow],
          Option[BrandRow]
        )
      ]] =
        inventoryCountBatches
          .drop((page - 1) * rowsPerPage)
          .take(rowsPerPage)
          .withCategoryAndBrand
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
      ): DBIO[Option[InventoryCountDTOProduct]] =
        inventoryCountProducts
          .filter(_.id === productId)
          .join(products)
          .on(_.productId === _.id)
          .result
          .headOption
          .map {
            _.map {
              case (batchProductRow, productRow) =>
                InventoryCountDTOProduct.fromRow(batchProductRow, productRow)
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
