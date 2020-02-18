package com.merit.modules.sales

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import com.merit.modules.products.{ProductRepo, SoldProductRow, ProductDTO, Currency, ProductID, ProductRow}
import com.merit.modules.brands.BrandID
import com.merit.modules.excel.ExcelSaleRow
import slick.jdbc.PostgresProfile
import scala.concurrent.Future
import PostgresProfile.api._
import org.joda.time.DateTime
import com.merit.modules.brands.BrandRepo
import slick.jdbc.JdbcBackend.Database
import com.merit.external.crawler.{SyncSaleResponse, CrawlerClient}
import collection.immutable.ListMap
import com.typesafe.scalalogging.LazyLogging
import com.merit.modules.salesEvents.SaleEventRepo
import com.merit.modules.users.UserID
import com.merit.modules.excel.ExcelWebSaleRow
import com.merit.modules.products.Sku
import com.merit.modules.products.Barcode
import com.merit.modules.brands.BrandRow

trait SaleService {
  def importSale(
    rows: Seq[ExcelSaleRow],
    date: DateTime,
    total: Currency,
    userId: UserID
  ): Future[SaleSummary]
  def importSalesFromWeb(
    rows: Seq[ExcelWebSaleRow],
    deductQuantities: Boolean = false
  ): Future[Seq[WebSaleSummary]]
  def create(
    total: Currency,
    discount: Currency,
    products: Seq[ProductDTO],
    userId: UserID
  ): Future[Option[SaleDTO]]
  def getSale(id: SaleID): Future[Option[SaleDTO]]
  def getSales(
    page: Int,
    rowsPerPage: Int,
    filters: SaleFilters = SaleFilters()
  ): Future[PaginatedSalesResponse]
  def saveSyncResult(result: SyncSaleResponse): Future[Seq[Int]]
}

object SaleService {
  def apply(
    db: Database,
    saleRepo: SaleRepo[DBIO],
    productRepo: ProductRepo[DBIO],
    saleEventRepo: SaleEventRepo[DBIO],
    brandRepo: BrandRepo[DBIO],
    crawlerClient: CrawlerClient
  )(implicit ec: ExecutionContext) = new SaleService with LazyLogging {
    private def insertFromExcel(
      rows: Seq[ExcelSaleRow],
      date: DateTime,
      total: Currency,
      outlet: SaleOutlet.Value = SaleOutlet.Store,
      userId: UserID
    ): Future[SaleSummary] = {
      logger.info(s"Inserting sale from excel with ${rows.length} rows")
      logger.info(s"Date: $date, Total: $total, Outlet: ${outlet.toString}")
      db.run(
        (for {
          soldProducts <- productRepo.findAll(rows.map(_.barcode))
          sale         <- saleRepo.insert(SaleRow(date, total, Currency(0), outlet))
          addedProducts <- saleRepo.addProductsToSale(
            soldProducts.map(
              p =>
                SoldProductRow(
                  p.id,
                  sale.id,
                  rows.find(_.barcode == p.barcode).map(_.qty).getOrElse(1)
                )
            )
          )
          _ <- DBIO.sequence(rows.map(r => productRepo.deductQuantity(r.barcode, r.qty)))
          _ <- saleEventRepo.insertSaleImportedEvent(
            sale.id,
            userId,
            rows.filter(p => soldProducts.find(_.barcode == p.barcode).isDefined),
            rows.filter(p => soldProducts.find(_.barcode == p.barcode).isEmpty),
            outlet
          )
        } yield
          SaleSummary(
            sale.id,
            sale.createdAt,
            sale.total,
            Currency(0),
            sale.outlet,
            sale.status,
            soldProducts.map(
              p =>
                SaleSummaryProduct.fromProductDTO(
                  p,
                  rows.find(_.barcode == p.barcode).map(_.qty).getOrElse(1)
                )
            )
          )).transactionally
      )
    }

    def importSale(
      rows: Seq[ExcelSaleRow],
      date: DateTime,
      total: Currency,
      userId: UserID
    ): Future[SaleSummary] =
      for {
        summary <- insertFromExcel(rows, date, total, SaleOutlet.Store, userId)
        _       <- crawlerClient.sendSale(summary)
      } yield (summary)

    def importSalesFromWeb(
      rows: Seq[ExcelWebSaleRow],
      deductQuantities: Boolean = false
    ): Future[Seq[WebSaleSummary]] = {
      val webSaleRows = rows.distinctByOrderNoWithoutProducts

      // This will be used only one time for the incomplete sales history data from web
      def createProductIfNotExists(
        row: ExcelWebSaleRow,
        brands: Seq[BrandRow]
      ): DBIO[(String, ProductRow, Int)] = {
        def doWork(
          row: ExcelWebSaleRow,
          b: String,
          s: String
        ): DBIO[(String, ProductRow, Int)] = {
          def toProductRow(row: ExcelWebSaleRow, b: String, s: String) = {
            import row._
            // format: off
            ProductRow(b, s, productName, price, None, 0, None, Some(tax), brands.find(_.name == brand).map(_.id), None)
            // format: on
          }

          productRepo.getRow(b).flatMap {
            case None =>
              productRepo.insert(toProductRow(row, b, s)).map(p => (row.orderNo, p, row.qty))
            case Some(pRow) => DBIO.successful(pRow).map(p => (row.orderNo, p, row.qty))
          }
        }

        import row._
        (barcode, sku) match {
          case (Some(b), Some(s)) => doWork(row, b, s)
          case (Some(b), None)    => doWork(row, b, Sku.random)
          case (None, Some(s))    => doWork(row, Barcode.random, s)
          case (None, None)       => doWork(row, Barcode.random, Sku.random)
        }
      }

      def findSaleId(sales: Seq[SaleRow], orderNo: String): SaleID =
        sales.find(_.orderNo == Some(orderNo)).map(_.id).getOrElse(sales.head.id)

      def makeDeduction(ps: Seq[(String, ProductRow, Int)], deductQuantities: Boolean) =
        if (deductQuantities) {
          logger.info(s"Deducting sold quantities from ${ps.length} products")
          DBIO.sequence(ps.map {
            case (_, product, soldQty) => productRepo.deductQuantity(product.barcode, soldQty)
          })
        } else DBIO.successful()

      db.run(
        (for {
          brands <- DBIO
            .sequence(rows.map(r => brandRepo.get(r.brand)))
            .map(_.flatten)

          _ = logger.info(s"Inserting ${rows.map(_.orderNo).distinct.length} sale records")
          sales <- saleRepo.batchInsert(
            webSaleRows.map(
              s =>
                SaleRow(
                  s.createdAt,
                  s.total,
                  s.discount,
                  SaleOutlet.Web,
                  s.status,
                  Some(s.orderNo)
                )
            )
          )
          _ = logger.info(
            s"${rows.filter(r => r.barcode.isEmpty && r.sku.isEmpty).length} products don't have sku or barcode"
          )
          products <- DBIO.sequence(rows.map(createProductIfNotExists(_, brands)))
          _ = logger.info(
            s"Adding ${products.length} products to sales"
          )
          soldProducts <- saleRepo.addProductsToSale(products.map {
            case (orderNo, product, soldQty) =>
              SoldProductRow(product.id, findSaleId(sales, orderNo), soldQty, true)
          })
          _ <- makeDeduction(products, deductQuantities)
        } yield
          (sales.map(
            s =>
              WebSaleSummary(
                s.orderNo.getOrElse(""),
                s.total,
                s.discount,
                s.createdAt,
                s.status,
                products
                  .filter(_._1 == s.orderNo.getOrElse(""))
                  .map(p => WebSaleSummaryProduct(p._2.sku, p._2.barcode, p._3))
              )
          ))).transactionally
      )
    }

    def create(
      total: Currency,
      discount: Currency,
      products: Seq[ProductDTO],
      userId: UserID
    ): Future[Option[SaleDTO]] = {
      logger.info(
        s"Creating sale with total: $total, discount: $discount, products: ${products.toString}"
      )
      val insertSale = db.run(
        (for {
          soldProducts <- productRepo.findAll(products.map(_.barcode))
          sale         <- saleRepo.insert(SaleRow(DateTime.now(), total, discount))
          _ <- saleRepo.addProductsToSale(
            products.map(p => SoldProductRow(p.id, sale.id, p.qty))
          )
          _ <- DBIO.sequence(products.map(p => productRepo.deductQuantity(p.barcode, p.qty)))
          _ <- saleEventRepo.insertSaleCreatedEvent(sale.id, userId)
        } yield
          SaleSummary(
            sale.id,
            sale.createdAt,
            sale.total,
            sale.discount,
            sale.outlet,
            sale.status,
            soldProducts.map(
              p =>
                SaleSummaryProduct
                  .fromProductDTO(
                    p,
                    products.find(_.barcode == p.barcode).map(_.qty).getOrElse(0)
                  )
            )
          )).transactionally
      )

      for {
        summary <- insertSale
        _       <- crawlerClient.sendSale(summary)
        dto     <- getSale(summary.id)
      } yield dto
    }

    def getSale(id: SaleID): Future[Option[SaleDTO]] =
      db.run(saleRepo.get(id)).map {
        case rows if rows.length < 1 => None
        case rows => {
          val products = rows.foldLeft(Seq[SaleDTOProduct]())(
            (s, p) => s :+ SaleDTOProduct.fromRow(p._2, p._5, p._6, p._4, p._3)
          )
          val sale = rows(0)._1
          Some(sale.toDTO(products))
        }
      }

    def getSales(
      page: Int,
      rowsPerPage: Int,
      filters: SaleFilters
    ): Future[PaginatedSalesResponse] =
      for {
        sales <- db.run(saleRepo.getAll(page, rowsPerPage, filters)).map {
          _.foldLeft(ListMap[SaleID, SaleDTO]()) {
            case (m, (saleRow, productRow, soldQty, synced, brand, category)) =>
              m + m
                .get(saleRow.id)
                .map(
                  sale =>
                    (sale.id -> sale.copy(
                      products = sale.products ++ Seq(
                        SaleDTOProduct
                          .fromRow(productRow, brand, category, synced, soldQty)
                      )
                    ))
                )
                .getOrElse(
                  saleRow.id -> saleRow.toDTO(
                    Seq(SaleDTOProduct.fromRow(productRow, brand, category, synced, soldQty))
                  )
                )
          }.values.toSeq
        }
        count <- db.run(saleRepo.count(filters))
      } yield PaginatedSalesResponse(count, sales)

    def saveSyncResult(result: SyncSaleResponse): Future[Seq[Int]] = {
      val synced = result.products.map(p => if (p.synced) 1 else 0).sum
      logger.info(s"Received sync sale response")
      logger.info(s"Synced $synced products out of ${result.products.length}")
      db.run(
        for {
          updated <- DBIO.sequence(
            result.products.map(p => saleRepo.syncSoldProduct(result.saleId, p.id, p.synced))
          )
          _ <- saleEventRepo
            .insertSaleSyncedEvent(result.saleId, result.products.length, synced)
        } yield updated
      )
    }
  }
}
