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
import com.merit.modules.common.CommonMethodsService
import slick.jdbc.JdbcBackend.Database
import com.merit.modules.products.ProductRow
import com.merit.modules.excel.ExcelInventoryCountRow
import com.merit.external.crawler.SyncInventoryCountResponse

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
  def cancel(batchId: InventoryCountBatchID): Future[Int]
  def complete(
    batchId: InventoryCountBatchID,
    force: Boolean = false
  ): Future[Either[String, InventoryCountDTO]]
  def delete(id: InventoryCountBatchID): Future[Boolean]
  def deleteProduct(id: InventoryCountProductID): Future[Boolean]
  def insertFromExcel(
    createdAt: DateTime,
    products: Seq[ExcelInventoryCountRow]
  ): Future[InventoryCountDTO]
  def saveSyncResult(result: SyncInventoryCountResponse): Future[Boolean]
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
      val commonMethodsService = CommonMethodsService(db, brandRepo, categoryRepo)

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

      def cancel(batchId: InventoryCountBatchID): Future[Int] = {
        logger.info(s"Cancelling inventory count batch with id $batchId")
        db.run(inventoryCountRepo.cancelInventoryCount(batchId))
      }

      def complete(
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
                // replace quantities of products in batch, 0 if not counted
                _ <- db.run(
                  DBIO.sequence(
                    products
                      .map(p => productRepo.replaceQuantity(p.barcode, p.counted.getOrElse(0)))
                  )
                )
                _ <- crawlerClient.sendInventoryCount(dto, products)
              } yield
                dto
                  .copy(
                    status = InventoryCountStatus.Completed,
                    finished = Some(DateTime.now())
                  )
                  .asRight
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
            case (counted, notCounted) if counted == 0 && !force =>
              Left("None of the products are counted.")
            case (counted, notCounted) if counted < notCounted && !force =>
              Left("Most of the products are not counted.")
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

      def delete(id: InventoryCountBatchID): Future[Boolean] =
        for {
          delP <- db.run(inventoryCountRepo.deleteAllInventoryCountProducts(id))
          delB <- db.run(inventoryCountRepo.deleteBatch(id))
        } yield (delB + delP) == 2

      def deleteProduct(id: InventoryCountProductID): Future[Boolean] =
        db.run(inventoryCountRepo.deleteInventoryCountProduct(id)).map(_ == 1)

      def insertFromExcel(
        createdAt: DateTime,
        products: Seq[ExcelInventoryCountRow]
      ): Future[InventoryCountDTO] = {
        logger.info(s"Inserting inventory count from excel with ${products.length} rows")
        val barcodeToQty =
          products.foldLeft(Map[String, Int]())((m, p) => m + (p.barcode -> p.qty))

        val existingProducts =
          DBIO.sequence(products.map(p => productRepo.get(p.barcode))).map(_.flatten)

        // * Create nonexisting products
        def createProductsDbio(
          batchId: InventoryCountBatchID,
          brandsMap: Map[String, BrandID],
          categoriesMap: Map[String, CategoryID]
        ): DBIO[Seq[InventoryCountProductRow]] =
          (for {
            existing <- existingProducts
            products <- productRepo.batchInsert(
              products
                .filterNot(p => existing.exists(_.barcode == p.barcode))
                .map(
                  p =>
                    ExcelInventoryCountRow.toProductRow(
                      p,
                      p.brand.flatMap(brandsMap.get(_)),
                      p.category.flatMap(categoriesMap.get(_))
                    )
                )
            )
            insertedAsInventoryCountProductRow <- inventoryCountRepo.addProductsToBatch(
              products.map(
                p =>
                  InventoryCountProductRow(
                    batchId,
                    p.id,
                    0,
                    DateTime.now(),
                    Some(p.qty),
                    false,
                    isNew = true
                  )
              )
            )
          } yield insertedAsInventoryCountProductRow).transactionally

        // * Update existing products

        def updateProductsDbio(
          batchId: InventoryCountBatchID
        ): DBIO[Seq[InventoryCountProductRow]] =
          (for {
            countedProducts <- existingProducts
              .map(_.map(p => (p, barcodeToQty.get(p.barcode).getOrElse(0))))
            _ <- DBIO.sequence(
              countedProducts.map(p => productRepo.replaceQuantity(p._1.barcode, p._2))
            )
            insertedAsInventoryCountProductRow <- inventoryCountRepo.addProductsToBatch(
              countedProducts.map {
                case (dto, qty) =>
                  InventoryCountProductRow(batchId, dto.id, dto.qty, DateTime.now(), Some(qty))
              }
            )
          } yield insertedAsInventoryCountProductRow).transactionally

        // * Create stock order
        // If only one brand and category add them to batch
        val createInventoryCountDbio = (for {
          brandsMap <- commonMethodsService.getBrandsMap(products.flatMap(_.brand).distinct)
          categoriesMap <- commonMethodsService.getCategoryMap(
            products.flatMap(_.category).distinct
          )
          batch <- inventoryCountRepo.insertBatch(
            InventoryCountBatchRow(
              createdAt,
              Some(createdAt),
              Some(s"Imported from excel at ${createdAt.toString("dd-MM-yyyy HH:mm")}"),
              if (categoriesMap.size == 1) Some(categoriesMap.values.head) else None,
              if (brandsMap.size == 1) Some(brandsMap.values.head) else None,
              InventoryCountStatus.Completed
            )
          )
          updatedProducts <- updateProductsDbio(batch.id)
          createdProducts <- createProductsDbio(batch.id, brandsMap, categoriesMap)
        } yield InventoryCountDTO.fromRow(batch, None, None)).transactionally

        for {
          summary  <- db.run(createInventoryCountDbio)
          products <- db.run(inventoryCountRepo.getAllProductsOfBatch(summary.id))
          _        <- crawlerClient.sendInventoryCount(summary, products)
        } yield summary
      }

      def saveSyncResult(result: SyncInventoryCountResponse): Future[Boolean] = {
        val synced = result.products.map(p => if (p.synced) 1 else 0).sum
        logger.info(s"Received sync inventory count response")
        logger.info(s"Synced $synced products out of ${result.products.length}")
        db.run(
            DBIO.sequence(
              result.products
                .map(p => inventoryCountRepo.syncProduct(p.id, p.synced))
            )
          )
          .map(_.sum)
          .map {
            case sum if sum == result.products.length => true
            case _ =>
              logger.info("Received product count and updated product count does not match")
              false
          }
      }
    }
}
