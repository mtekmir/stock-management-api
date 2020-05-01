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
import com.typesafe.scalalogging.LazyLogging

trait InventoryCountService {
  def create(
    startDate: Option[DateTime],
    name: Option[String],
    brand: Option[BrandID],
    category: Option[CategoryID]
  ): Future[InventoryCountDTO]
  def getBatch(id: InventoryCountBatchID): Future[Option[InventoryCountDTO]]
  def getBatches(
    page: Int,
    rowsPerPage: Int,
    status: InventoryCountStatus
  ): Future[PaginatedInventoryCountBatchesResponse]
  def getBatchProducts(
    batchId: InventoryCountBatchID,
    status: InventoryCountProductStatus,
    page: Int,
    rowsPerPage: Int
  ): Future[PaginatedInventoryCountProductsResponse]
  def searchBatchProducts(
    batchId: InventoryCountBatchID,
    query: String
  ): Future[Seq[InventoryCountProductDTO]]
  def countProduct(
    productId: InventoryCountProductID,
    count: Int
  ): Future[Option[InventoryCountProductDTO]]
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
    new InventoryCountService with LazyLogging {
      def create(
        startDate: Option[DateTime],
        name: Option[String],
        brandId: Option[BrandID],
        categoryId: Option[CategoryID]
      ): Future[InventoryCountDTO] = {
        logger.info(
          s"Creating inventory count batch with brandId: $brandId, categoryId: $categoryId"
        )
        db.run(
          for {
            batch <- inventoryCountRepo.insertBatch(
              InventoryCountBatchRow(
                startDate.getOrElse(DateTime.now()),
                None,
                name,
                categoryId,
                brandId
              )
            )
            products <- productRepo.getAll(ProductFilters(categoryId, brandId))
            brand    <- brandRepo.get(brandId)
            category <- categoryRepo.get(categoryId)
            _ <- inventoryCountRepo.addProductsToBatch(
              products.map(_.toInventoryCountProductRow(batch.id))
            )
          } yield InventoryCountDTO.fromRow(batch, category, brand)
        )
      }

      def getBatch(id: InventoryCountBatchID): Future[Option[InventoryCountDTO]] =
        db.run(inventoryCountRepo.get(id)).map {
          _.map {
            case (batch, category, brand) => InventoryCountDTO.fromRow(batch, category, brand)
          }
        }

      def getBatches(
        page: Int,
        rowsPerPage: Int,
        status: InventoryCountStatus
      ): Future[PaginatedInventoryCountBatchesResponse] =
        for {
          batches <- db.run(inventoryCountRepo.getAll(page, rowsPerPage, status)).map {
            _.map {
              case (batch, category, brand) =>
                InventoryCountDTO.fromRow(batch, category, brand)
            }
          }
          count <- db.run(inventoryCountRepo.count(status))
        } yield PaginatedInventoryCountBatchesResponse(count, batches)

      def getBatchProducts(
        batchId: InventoryCountBatchID,
        status: InventoryCountProductStatus,
        page: Int,
        rowsPerPage: Int
      ): Future[PaginatedInventoryCountProductsResponse] =
        for {
          products <- db.run(
            inventoryCountRepo.getBatchProducts(batchId, status, page, rowsPerPage)
          )
          counted   <- db.run(inventoryCountRepo.productCount(batchId, counted = true))
          uncounted <- db.run(inventoryCountRepo.productCount(batchId, counted = false))
        } yield PaginatedInventoryCountProductsResponse(counted, uncounted, products)

      def searchBatchProducts(
        batchId: InventoryCountBatchID,
        query: String
      ): Future[Seq[InventoryCountProductDTO]] =
        db.run(inventoryCountRepo.searchBatchProducts(batchId, query))

      def countProduct(
        productId: InventoryCountProductID,
        count: Int
      ): Future[Option[InventoryCountProductDTO]] = {
        logger.info(s"Counted $count of product with id $productId")
        for {
          _       <- db.run(inventoryCountRepo.countProduct(productId, count))
          product <- db.run(inventoryCountRepo.getProduct(productId))
        } yield (product)
      }

      def cancelInventoryCount(batchId: InventoryCountBatchID): Future[Int] = {
        logger.info(s"Cancelling inventory count batch with id $batchId")
        db.run(inventoryCountRepo.cancelInventoryCount(batchId))
      }

      def completeInventoryCount(
        batchId: InventoryCountBatchID
      ): Future[Option[InventoryCountDTO]] = {
        logger.info(s"Completing inventory count batch with id $batchId")
        (for {
          inventoryCount <- OptionT(getBatch(batchId))
          _              <- OptionT.liftF(db.run(inventoryCountRepo.completeInventoryCount(batchId)))
          _              <- OptionT.liftF(crawlerClient.sendInventoryCount(inventoryCount))
        } yield inventoryCount).value
      }
    }
}
