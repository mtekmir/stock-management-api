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
import cats.implicits._
import scala.collection.immutable.ListMap
import com.typesafe.scalalogging.LazyLogging
import com.merit.modules.excel.ExcelStockOrderRow

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
  def completeInventoryCount(
    batchId: InventoryCountBatchID,
    force: Boolean = false
  ): Future[Either[String, InventoryCountDTO]]
  def deleteInventoryCount(id: InventoryCountBatchID): Future[Boolean]
  def deleteInventoryCountProduct(id: InventoryCountProductID): Future[Boolean]
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
        batchId: InventoryCountBatchID,
        force: Boolean = false
      ): Future[Either[String, InventoryCountDTO]] = {
        logger.info(s"Completing inventory count batch with id $batchId")
        def completeInventoryCountAction(
          maybeDTO: Option[InventoryCountDTO]
        ): Future[Either[String, InventoryCountDTO]] =
          maybeDTO match {
            case Some(dto) =>
              for {
                _        <- db.run(inventoryCountRepo.completeInventoryCount(batchId))
                products <- db.run(inventoryCountRepo.getAllProductsOfBatch(batchId))
                _        <- crawlerClient.sendInventoryCount(dto, products)
              } yield dto.asRight
            case None => Future.successful(Left("Inventory count batch not found"))
          }

        def isOkToComplete(
          batchId: InventoryCountBatchID,
          force: Boolean
        ): Future[Either[String, Boolean]] =
          (for {
            numberOfCounted    <- db.run(inventoryCountRepo.productCount(batchId, true))
            numberOfNotCounted <- db.run(inventoryCountRepo.productCount(batchId, false))
          } yield (numberOfCounted, numberOfNotCounted)).map {
            case (counted, notCounted) if counted < notCounted && !force =>
              Left("Most of the products are not counted.")
            case (counted, notCounted) if counted == 0 && !force =>
              Left("None of the items are counted.")
            case _ => Right(true)
          }

        for {
          inventoryCount <- getBatch(batchId)
          isOk           <- isOkToComplete(batchId, force)
          result <- isOk match {
            case Left(value) => Future.successful(Left(value))
            case Right(_)    => completeInventoryCountAction(inventoryCount)
          }
        } yield result

      }

      def deleteInventoryCount(id: InventoryCountBatchID): Future[Boolean] =
        for {
          delP <- db.run(inventoryCountRepo.deleteAllInventoryCountProducts(id))
          delB <- db.run(inventoryCountRepo.deleteBatch(id))
        } yield (delB + delP) == 2

      def deleteInventoryCountProduct(id: InventoryCountProductID): Future[Boolean] =
        db.run(inventoryCountRepo.deleteInventoryCountProduct(id)).map(_ == 1)

    }
}
