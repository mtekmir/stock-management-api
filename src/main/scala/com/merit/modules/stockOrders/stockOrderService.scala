package com.merit.modules.stockOrders

import slick.jdbc.JdbcBackend.Database
import slick.dbio.DBIO
import scala.concurrent.Future
import com.merit.modules.excel.ExcelStockOrderRow
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import com.merit.modules.products.OrderedProductRow
import com.merit.modules.products.ProductRepo
import slick.jdbc.PostgresProfile.api._
import com.merit.modules.products.ProductDTO
import com.merit.modules.brands.BrandRepo
import com.merit.modules.categories.CategoryRepo
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow
import com.merit.external.crawler.{CrawlerClient, SyncStockOrderResponse}
import com.typesafe.scalalogging.LazyLogging
import scala.collection.immutable.ListMap
import com.merit.modules.common.CommonMethodsService

trait StockOrderService {
  def getStockOrder(id: StockOrderID): Future[Option[StockOrderDTO]]
  def getStockOrders(): Future[Seq[StockOrderDTO]]
  def insertFromExcel(
    createdAt: DateTime,
    products: Seq[ExcelStockOrderRow]
  ): Future[StockOrderSummary]
  def saveSyncResult(result: SyncStockOrderResponse): Future[Seq[Int]]
}

object StockOrderService {
  def apply(
    db: Database,
    stockOrderRepo: StockOrderRepo[DBIO],
    productRepo: ProductRepo[DBIO],
    brandRepo: BrandRepo[DBIO],
    categoryRepo: CategoryRepo[DBIO],
    crawlerClient: CrawlerClient
  )(implicit ec: ExecutionContext): StockOrderService =
    new StockOrderService with LazyLogging {
      val commonMethodsService = CommonMethodsService(db, brandRepo, categoryRepo)

      def getStockOrder(id: StockOrderID): Future[Option[StockOrderDTO]] =
        db.run(stockOrderRepo.get(id)).map {
          case rows if rows.length < 1 => None
          case rows =>
            val products = rows
              .foldLeft(Seq[StockOrderDTOProduct]()) {
                case (s, (so, p, ordered, synced, brand, category)) =>
                  s :+ StockOrderDTOProduct.fromRow(p, brand, category, ordered, synced)
              }
            Some(StockOrderDTO(rows(0)._1.id, rows(0)._1.date, products))
        }

      def getStockOrders(): Future[Seq[StockOrderDTO]] =
        db.run(stockOrderRepo.getAll().map {
          _.foldLeft(ListMap[StockOrderID, StockOrderDTO]()) {
            case (m, (so, p, ordered, synced, brand, category)) =>
              m + m
                .get(so.id)
                .map(
                  dto =>
                    so.id -> dto.copy(
                      products = dto.products ++ Seq(
                        StockOrderDTOProduct
                          .fromRow(p, brand, category, ordered, synced)
                      )
                    )
                )
                .getOrElse(
                  so.id -> StockOrderDTO(
                    so.id,
                    so.date,
                    Seq(StockOrderDTOProduct.fromRow(p, brand, category, ordered, synced))
                  )
                )
          }.values.toSeq
        })

      def insertFromExcel(
        createdAt: DateTime,
        products: Seq[ExcelStockOrderRow]
      ): Future[StockOrderSummary] = {
        logger.info(s"Inserting stock order from excel with ${products.length} rows")
        // todo test the case of duplicate products
        val barcodeToQty =
          products.foldLeft(Map[String, Int]())((m, p) => m + (p.barcode -> p.qty))

        val existingProducts =
          DBIO.sequence(products.map(p => productRepo.get(p.barcode))).map(_.flatten)

        // * Create nonexisting products

        val createProductsDbio: DBIO[Seq[StockOrderSummaryProduct]] = (for {
          brandsMap <- DBIO
            .sequence(products.flatMap(_.brand).map(commonMethodsService.insertBrandIfNotExists(_)))
            .map(
              _.foldLeft(Map[String, BrandID]())((m, b) => m + (b.name -> b.id))
            )

          categoriesMap <- DBIO
            .sequence(products.flatMap(_.category).map(commonMethodsService.insertCategoryIfNotExists(_)))
            .map(
              _.foldLeft(Map[String, CategoryID]())((m, c) => m + (c.name -> c.id))
            )

          existing <- existingProducts

          products <- productRepo.batchInsert(
            products
              .filter(p => existing.filter(_.barcode == p.barcode).length == 0)
              .map(
                p =>
                  ExcelStockOrderRow.toProductRow(
                    p,
                    p.brand.flatMap(brandsMap.get(_)),
                    p.category.flatMap(categoriesMap.get(_))
                  )
              )
          )
        } yield products.map(StockOrderSummaryProduct.fromProductRow(_))).transactionally

        // * Update existing products

        val updateProductsDbio: DBIO[Seq[StockOrderSummaryProduct]] = (for {
          orderedProducts <- existingProducts
            .map(_.map(p => (p, barcodeToQty.get(p.barcode).getOrElse(1))))
          _ <- DBIO.sequence(
            orderedProducts.map(p => productRepo.addQuantity(p._1.barcode, p._2))
          )
        } yield
          orderedProducts.map(
            p =>
              StockOrderSummaryProduct.fromProductDTO(p._1, prevQty = p._1.qty, ordered = p._2)
          )).transactionally

        // * Create stock order
        val createStockOrderDbio = (for {
          stockOrder      <- stockOrderRepo.insert(StockOrderRow(createdAt))
          updatedProducts <- updateProductsDbio
          createdProducts <- createProductsDbio
          _ <- stockOrderRepo.addProductsToStockOrder(
            (createdProducts ++ updatedProducts).map(
              p =>
                OrderedProductRow(
                  p.id,
                  stockOrder.id,
                  p.ordered
                )
            )
          )
        } yield
          StockOrderSummary(
            stockOrder.id,
            stockOrder.date,
            created = createdProducts,
            updated = updatedProducts
          )).transactionally

        for {
          summary <- db.run(createStockOrderDbio.transactionally)
          _       <- crawlerClient.sendStockOrder(summary)
        } yield summary
      }

      def saveSyncResult(result: SyncStockOrderResponse): Future[Seq[Int]] = {
        val synced = result.products.map(p => if (p.synced) 1 else 0).sum
        logger.info(s"Received sync stock order response")
        logger.info(s"Synced $synced products out of ${result.products.length}")
        db.run(
          DBIO.sequence(
            result.products
              .map(p => stockOrderRepo.syncOrderedProduct(result.stockOrderId, p.id, p.synced))
          )
        )
      }
    }
}
