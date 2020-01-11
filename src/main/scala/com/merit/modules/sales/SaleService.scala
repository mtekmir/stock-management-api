package com.merit.modules.sales

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import com.merit.modules.products.{ProductRepo, SoldProductRow, ProductDTO}
import com.merit.modules.excel.ExcelSaleRow
import slick.jdbc.PostgresProfile
import scala.concurrent.Future
import PostgresProfile.api._
import org.joda.time.DateTime
import com.merit.modules.brands.BrandRepo
import slick.jdbc.JdbcBackend.Database
import com.merit.external.crawler.SyncSaleResponse
import com.merit.external.crawler.CrawlerClient
import com.merit.modules.products.Currency
import collection.immutable.ListMap

trait SaleService {
  def insertFromExcel(
    rows: Seq[ExcelSaleRow],
    date: DateTime,
    total: Currency
  ): Future[SaleSummary]
  def getSale(id: SaleID): Future[Option[SaleDTO]]
  def getSales(page: Int, rowsPerPage: Int): Future[PaginatedSalesResponse]
  def saveSyncResult(result: SyncSaleResponse): Future[Seq[Int]]
}

object SaleService {
  def apply(
    db: Database,
    saleRepo: SaleRepo[DBIO],
    productRepo: ProductRepo[DBIO],
    crawlerClient: CrawlerClient
  )(implicit ec: ExecutionContext) = new SaleService {
    def insertFromExcel(
      rows: Seq[ExcelSaleRow],
      date: DateTime,
      total: Currency
    ): Future[SaleSummary] = {
      val insertSale = db.run(
        (for {
          soldProducts <- productRepo.findAll(rows.map(_.barcode))
          sale         <- saleRepo.add(SaleRow(date, total))
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
            soldProducts.map(
              p =>
                SaleSummaryProduct.fromProductDTO(
                  p,
                  rows.find(_.barcode == p.barcode).map(_.qty).getOrElse(1)
                )
            )
          )).transactionally
      )

      for {
        summary <- insertSale
        _       <- crawlerClient.sendSale(summary)
      } yield (summary)
    }

    def getSale(id: SaleID): Future[Option[SaleDTO]] =
      db.run(saleRepo.get(id)).map {
        case rows if rows.length < 1 => None
        case rows => {
          val products = rows.foldLeft(Seq[SaleDTOProduct]())(
            (s, p) => s :+ SaleDTOProduct.fromRow(p._2, p._5, p._6, p._4, p._3)
          )
          val sale = rows(0)._1
          Some(SaleDTO(sale.id, sale.createdAt, sale.total, products))
        }
      }

    def getSales(page: Int, rowsPerPage: Int): Future[PaginatedSalesResponse] =
      for {
        sales <- db.run(saleRepo.getAll(page, rowsPerPage)).map {
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
                    saleRow.total,
                    Seq(SaleDTOProduct.fromRow(productRow, brand, category, synced, soldQty))
                  )
                )
          }.values.toSeq
        }
        count <- db.run(saleRepo.count)
      } yield PaginatedSalesResponse(count, sales)

    def saveSyncResult(result: SyncSaleResponse): Future[Seq[Int]] =
      db.run(
        DBIO.sequence(
          result.products.map(p => saleRepo.syncSoldProduct(result.saleId, p.id, p.synced))
        )
      )
  }
}
