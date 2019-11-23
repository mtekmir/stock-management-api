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

trait StockOrderService {
  def get(id: StockOrderID): Future[Option[StockOrderDTO]]
  def insertFromExcel(
    createdAt: DateTime,
    products: Seq[ExcelStockOrderRow]
  ): Future[StockOrderSummary]
}

object StockOrderService {
  def apply(
    db: Database,
    stockOrderRepo: StockOrderRepo[DBIO],
    productRepo: ProductRepo[DBIO],
    brandRepo: BrandRepo[DBIO],
    categoryRepo: CategoryRepo[DBIO]
  )(implicit ec: ExecutionContext): StockOrderService = new StockOrderService {
    def get(id: StockOrderID): Future[Option[StockOrderDTO]] =
      db.run(stockOrderRepo.get(id)).map {
        case rows if rows.length < 1 => None
        case rows =>
          val products = rows
            .foldLeft(Seq[ProductDTO]())(
              (s, p) => s :+ ProductDTO.fromRow(p._2, p._4, p._5)
            )
          Some(StockOrderDTO(rows(0)._1.id, rows(0)._1.date, products))
      }

    def insertFromExcel(
      createdAt: DateTime,
      products: Seq[ExcelStockOrderRow]
    ): Future[StockOrderSummary] = {
      val barcodeToQty =
        products.foldLeft(Map[String, Int]())((m, p) => m + (p.barcode -> p.qty))

      val existingProducts = products.map(p => productRepo.get(p.barcode))

      // * Create nonexisting products
      val productsToCreate = products.zip(existingProducts).map {
        case (product, dbio) =>
          dbio.collect {
            case None => product
          }
      }

      val createProductsDbio: DBIO[Seq[StockOrderSummaryProduct]] = (for {
        brandsMap <- brandRepo.getAll.map(
          _.foldLeft(Map[String, BrandID]())((m, b) => m + (b.name -> b.id))
        )
        categoriesMap <- categoryRepo.getAll.map(
          _.foldLeft(Map[String, CategoryID]())((m, c) => m + (c.name -> c.id))
        )
        psToCreate <- DBIO.sequence(productsToCreate.map {
          _.map(
            p =>
              ExcelStockOrderRow.toProductRow(
                p,
                p.brand.flatMap(brandsMap.get(_)),
                p.category.flatMap(categoriesMap.get(_))
              )
          )
        })
        products <- productRepo.batchInsert(psToCreate)
      } yield products.map(StockOrderSummaryProduct.fromProductRow(_))).transactionally

      // * Update existing products
      val productsToUpdate = barcodeToQty
        .zip(existingProducts)
        .map {
          case ((barcode, qty), dbio) =>
            dbio.collect {
              case Some(product) => (product, qty)
            }
        }
        .toSeq

      val updateProductsDbio: DBIO[Seq[StockOrderSummaryProduct]] = (for {
        orderedProducts <- DBIO.sequence(productsToUpdate)
        _               <- DBIO.sequence(products.map(p => productRepo.addQuantity(p.barcode, p.qty)))
      } yield
        orderedProducts.map(
          p => StockOrderSummaryProduct.fromProductDTO(p._1, prevQty = p._1.qty - p._2)
        )).transactionally

      // * Create stock order
      val createStockOrderDbio = (for {
        stockOrder      <- stockOrderRepo.add(StockOrderRow(createdAt))
        createdProducts <- createProductsDbio
        updatedProducts <- updateProductsDbio
        _ <- stockOrderRepo.addProductsToStockOrder(
          (createdProducts ++ updatedProducts).map(
            p =>
              OrderedProductRow(
                p.id,
                stockOrder.id,
                p.newQty - p.prevQty
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

      db.run(createStockOrderDbio.transactionally)
    }
  }
}
