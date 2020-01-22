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

trait SaleService {
  def importSale(
    rows: Seq[ExcelSaleRow],
    date: DateTime,
    total: Currency
  ): Future[SaleSummary]
  def importSoldProductsFromWeb(
    rows: Seq[ExcelSaleRow]
  ): Future[SaleSummary]
  def create(
    total: Currency,
    discount: Currency,
    products: Seq[ProductDTO]
  ): Future[SaleSummary]
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
    crawlerClient: CrawlerClient
  )(implicit ec: ExecutionContext) = new SaleService {
    private def insertFromExcel(
      rows: Seq[ExcelSaleRow],
      date: DateTime,
      total: Currency,
      outlet: SaleOutlet.Value = SaleOutlet.Store
    ): Future[SaleSummary] =
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
        } yield
          SaleSummary(
            sale.id,
            sale.createdAt,
            sale.total,
            Currency(0),
            sale.outlet,
            soldProducts.map(
              p =>
                SaleSummaryProduct.fromProductDTO(
                  p,
                  rows.find(_.barcode == p.barcode).map(_.qty).getOrElse(1)
                )
            )
          )).transactionally
      )

    def importSale(
      rows: Seq[ExcelSaleRow],
      date: DateTime,
      total: Currency
    ): Future[SaleSummary] =
      for {
        summary <- insertFromExcel(rows, date, total)
        _       <- crawlerClient.sendSale(summary)
      } yield (summary)

    def importSoldProductsFromWeb(
      rows: Seq[ExcelSaleRow]
    ): Future[SaleSummary] =
      insertFromExcel(rows, DateTime.now(), Currency(0), SaleOutlet.Web)

    def create(
      total: Currency,
      discount: Currency,
      products: Seq[ProductDTO]
    ): Future[SaleSummary] = {
      val insertSale = db.run(
        (for {
          sale <- saleRepo.insert(SaleRow(DateTime.now(), total, discount))
          _ <- saleRepo.addProductsToSale(
            products.map(p => SoldProductRow(p.id, sale.id, p.qty))
          )
          _ <- DBIO.sequence(products.map(p => productRepo.deductQuantity(p.barcode, p.qty)))
        } yield
          SaleSummary(
            sale.id,
            sale.createdAt,
            sale.total,
            sale.discount,
            sale.outlet,
            products.map(p => SaleSummaryProduct.fromProductDTO(p, p.qty))
          )).transactionally
      )

      for {
        summary <- insertSale
        _       <- crawlerClient.sendSale(summary)
      } yield summary
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
            SaleDTO(sale.id, sale.createdAt, sale.outlet, sale.total, sale.discount, products)
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
                    saleRow.total,
                    saleRow.discount,
                    Seq(SaleDTOProduct.fromRow(productRow, brand, category, synced, soldQty))
                  )
                )
          }.values.toSeq
        }
        count <- db.run(saleRepo.count(filters))
      } yield PaginatedSalesResponse(count, sales)
    

    def saveSyncResult(result: SyncSaleResponse): Future[Seq[Int]] =
      db.run(
        DBIO.sequence(
          result.products.map(p => saleRepo.syncSoldProduct(result.saleId, p.id, p.synced))
        )
      )
  }
}
