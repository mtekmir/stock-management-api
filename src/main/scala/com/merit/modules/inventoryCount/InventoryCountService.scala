package com.merit.modules.inventoryCount

import scala.concurrent.Future
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID
import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIO
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import com.merit.modules.products.ProductRepo
import com.merit.modules.products.ProductFilters
import com.merit.modules.brands.BrandRepo
import com.merit.modules.categories.CategoryRepo
import com.merit.external.crawler.CrawlerClient
import cats.data.OptionT
import cats.instances.future._
import scala.collection.immutable.ListMap

trait InventoryCountService {
  def create(
    name: Option[String],
    brand: Option[BrandID],
    category: Option[CategoryID]
  ): Future[InventoryCountDTO]
  def getBatch(id: InventoryCountBatchID): Future[Option[InventoryCountDTO]]
  def getBatches(page: Int, rowsPerPage: Int): Future[PaginatedInventoryCountBatchesResponse]
  def countProduct(
    productId: InventoryCountProductID,
    count: Int
  ): Future[Option[InventoryCountDTOProduct]]
  def cancelInventoryCount(batchId: InventoryCountBatchID): Future[Int]
  def completeInventoryCount(batchId: InventoryCountBatchID): Future[Option[InventoryCountDTO]]
}

object InventoryCountService {
  def apply(
    db: Database,
    inventoryCountRepo: InventoryCountRepo[DBIO],
    productRepo: ProductRepo[DBIO],
    brandRepo: BrandRepo[DBIO],
    categoryRepo: CategoryRepo[DBIO],
    crawlerClient: CrawlerClient
  )(
    implicit ec: ExecutionContext
  ): InventoryCountService =
    new InventoryCountService {
      def create(
        name: Option[String],
        brandId: Option[BrandID],
        categoryId: Option[CategoryID]
      ): Future[InventoryCountDTO] =
        db.run(
          for {
            batch <- inventoryCountRepo.insertBatch(
              InventoryCountBatchRow(DateTime.now(), None, name, categoryId, brandId)
            )
            products <- productRepo.getAll(ProductFilters(categoryId, brandId))
            brand    <- brandRepo.get(brandId)
            category <- categoryRepo.get(categoryId)
            _ <- inventoryCountRepo.addProductsToBatch(
              products.map(_.toInventoryCountProductRow(batch.id))
            )
          } yield
            InventoryCountDTO.fromRow(batch, products, brand.map(_.name), category.map(_.name))
        )

      def getBatch(id: InventoryCountBatchID): Future[Option[InventoryCountDTO]] =
        db.run(
          inventoryCountRepo.get(id).map {
            case Nil => None
            case rows =>
              val products = rows.foldLeft(Seq[InventoryCountDTOProduct]()) {
                case (s, (_, _, _, batchProduct, product)) =>
                  s :+ InventoryCountDTOProduct.fromRow(batchProduct, product)
              }
              Some(InventoryCountDTO.fromRow(rows(0)._1, rows(0)._2, rows(0)._3, products))
          }
        )

      def getBatches(
        page: Int,
        rowsPerPage: Int
      ): Future[PaginatedInventoryCountBatchesResponse] =
        for {
          batches <- db.run(inventoryCountRepo.getAll(page, rowsPerPage).map {
            _.foldLeft(ListMap[InventoryCountBatchID, InventoryCountDTO]()) {
              case (m, (batchRow, categoryRow, brandRow, batchProductRow, productRow)) =>
                m + m
                  .get(batchRow.id)
                  .map(
                    dto =>
                      (dto.id -> dto.copy(
                        products = dto.products ++ Seq(
                          InventoryCountDTOProduct.fromRow(batchProductRow, productRow)
                        )
                      ))
                  )
                  .getOrElse(
                    batchRow.id -> InventoryCountDTO.fromRow(
                      batchRow,
                      categoryRow,
                      brandRow,
                      Seq(InventoryCountDTOProduct.fromRow(batchProductRow, productRow))
                    )
                  )
            }.values.toSeq
          })
          count <- db.run(inventoryCountRepo.count)
        } yield PaginatedInventoryCountBatchesResponse(count, batches)

      def countProduct(
        productId: InventoryCountProductID,
        count: Int
      ): Future[Option[InventoryCountDTOProduct]] =
        for {
          _       <- db.run(inventoryCountRepo.countProduct(productId, count))
          product <- db.run(inventoryCountRepo.getProduct(productId))
        } yield (product)

      def cancelInventoryCount(batchId: InventoryCountBatchID): Future[Int] =
        db.run(inventoryCountRepo.cancelInventoryCount(batchId))

      def completeInventoryCount(
        batchId: InventoryCountBatchID
      ): Future[Option[InventoryCountDTO]] =
        (for {
          inventoryCount <- OptionT(getBatch(batchId))
          _              <- OptionT.liftF(db.run(inventoryCountRepo.completeInventoryCount(batchId)))
          _              <- OptionT.liftF(crawlerClient.sendInventoryCount(inventoryCount))
        } yield inventoryCount).value
    }
}
