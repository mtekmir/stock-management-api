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

trait SaleService {
  def getExcelImportSummary(row: Seq[ExcelSaleRow]): Future[SaleSummary]
  def insertFromExcel(rows: Seq[ExcelSaleRow], date: DateTime): Future[SaleDTO]
  def getSale(id: SaleID): Future[Option[SaleDTO]]
}

object SaleService {
  def apply(
    db: PostgresProfile.backend.Database,
    saleRepo: SaleRepo[DBIO],
    productRepo: ProductRepo[DBIO]
  )(implicit ec: ExecutionContext) = new SaleService {
    def getExcelImportSummary(
      rows: Seq[ExcelSaleRow]
    ): Future[SaleSummary] = { // * Runs after the import
      val excelRowsMap =
        rows.foldLeft(Map[String, Int]())((m, r) => (m + (r.barcode -> r.qty)))
      val productsWithCurrentQty = rows.map(r => productRepo.get(r.barcode))

      val dbios = excelRowsMap.zip(productsWithCurrentQty).map {
        case (((barcode, soldQty), dbio)) =>
          dbio.map {
            case None          => SaleSummaryProduct(barcode)
            case Some(product) => SaleSummaryProduct.fromProductRow(product, soldQty)
          }
      }

      db.run(DBIO.sequence(dbios)).map(p => SaleSummary(p.toSeq))
    }

    def insertFromExcel(rows: Seq[ExcelSaleRow], date: DateTime): Future[SaleDTO] = {
      val dbio: DBIO[SaleDTO] = for {
        soldProducts <- productRepo.findAll(rows.map(_.barcode))
        sale         <- saleRepo.add(SaleRow(date))
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
      } yield SaleDTO(sale.id, sale.createdAt, soldProducts)

      db.run(dbio.transactionally)
    }

    def getSale(id: SaleID): Future[Option[SaleDTO]] =
      db.run(saleRepo.get(id)).map {
        case rows if rows.length < 1 => None
        case rows =>
          val products = rows.foldLeft(Seq[ProductDTO]())(
            (s, p) => s :+ ProductDTO.fromRow(p._2, p._4, p._5).copy(qty = p._3)
          )
          Some(SaleDTO(rows(0)._1.id, rows(0)._1.createdAt, products))
      }
  }
}
