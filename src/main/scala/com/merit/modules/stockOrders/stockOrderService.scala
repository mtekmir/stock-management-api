package com.merit.modules.stockOrders

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

trait StockOrderService {
  def get(id: StockOrderID): Future[Option[StockOrderDTO]]
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
  )(implicit ec: ExecutionContext): StockOrderService = new StockOrderService {
    private def insertBrandIfNotExists(name: String) =
      brandRepo.getByName(name).flatMap {
        case None        => brandRepo.insert(BrandRow(name))
        case Some(brand) => DBIO.successful(brand)
      }

    private def insertCategoryIfNotExists(name: String) =
      categoryRepo.getByName(name).flatMap {
        case None           => categoryRepo.insert(CategoryRow(name))
        case Some(category) => DBIO.successful(category)
      }

    def get(id: StockOrderID): Future[Option[StockOrderDTO]] =
      db.run(stockOrderRepo.get(id)).map {
        case rows if rows.length < 1 => None
        case rows =>
          val products = rows
            .foldLeft(Seq[StockOrderDTOProduct]())(
              (s, p) =>
                s :+ StockOrderDTOProduct.fromRow(p._2, p._5, p._6, p._4).copy(qty = p._3)
            )
          Some(StockOrderDTO(rows(0)._1.id, rows(0)._1.date, products))
      }

    def insertFromExcel(
      createdAt: DateTime,
      products: Seq[ExcelStockOrderRow]
    ): Future[StockOrderSummary] = {
      // todo test the case of duplicate products
      val barcodeToQty =
        products.foldLeft(Map[String, Int]())((m, p) => m + (p.barcode -> p.qty))

      val existingProducts =
        DBIO.sequence(products.map(p => productRepo.get(p.barcode))).map(_.flatten)

      // * Create nonexisting products

      val createProductsDbio: DBIO[Seq[StockOrderSummaryProduct]] = (for {
        brandsMap <- DBIO
          .sequence(products.flatMap(_.brand).map(insertBrandIfNotExists(_)))
          .map(
            _.foldLeft(Map[String, BrandID]())((m, b) => m + (b.name -> b.id))
          )

        categoriesMap <- DBIO
          .sequence(products.flatMap(_.category).map(insertCategoryIfNotExists(_)))
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
        stockOrder      <- stockOrderRepo.add(StockOrderRow(createdAt))
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

    def saveSyncResult(result: SyncStockOrderResponse): Future[Seq[Int]] =
      db.run(
        DBIO.sequence(
          result.products
            .map(p => stockOrderRepo.syncOrderedProduct(result.stockOrderId, p.id, p.synced))
        )
      )
  }
}
