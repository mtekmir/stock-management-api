package com.merit.modules.sales

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import com.merit.modules.products.{ProductRepo, SoldProductRow, ProductDTO, Currency}
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

trait SaleService {
  def importSale(
    rows: Seq[ExcelSaleRow],
    date: DateTime,
    total: Currency,
    userId: UserID
  ): Future[SaleSummary]
  def importSalesFromWeb(
    rows: Seq[ExcelWebSaleRow]
  ): Future[Seq[SaleSummary]]
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

    def importSalesFromWeb(rows: Seq[ExcelWebSaleRow]): Future[Seq[SaleSummary]] = {
      // def groupSalesAndProducts(rows: Seq[ExcelWebSaleRow]): Seq[SaleDTO] =
      //   rows.foldLeft(ListMap[String, SaleDTO]()) {
      //     case (
      //         m,
      //         ExcelWebSaleRow(id, total, discount, createdAt, pName, sku, brand, barcode, qty)
      //         ) =>
      //       m + m.get(id).map(dto => 
      //         id -> dto.copy(products = dto.products ++ Seq(SaleDTOProduct(id,)))
      //       ).getOrElse(SaleDTO(id, createdAt, SaleOutlet.Web, total, discount, Seq(SaleDTOProduct())))
      //   }

      ???
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
          Some(
            SaleDTO(sale.id, sale.createdAt, sale.outlet, sale.status, sale.total, sale.discount, products)
          )
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
                  saleRow.id -> SaleDTO(
                    saleRow.id,
                    saleRow.createdAt,
                    saleRow.outlet,
                    saleRow.status,
                    saleRow.total,
                    saleRow.discount,
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
