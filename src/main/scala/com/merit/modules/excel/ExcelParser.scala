package com.merit.modules.excel
import com.merit.modules.products.Currency
import scala.util.Try

trait ExcelParser {
  def parseProductRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelProductRow]
  def parseSaleRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelSaleRow]
  def parseStockOrderRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelStockOrderRow]
}

object ExcelParser {
  def apply() = new ExcelParser {
    def parseProductRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelProductRow] =
      rows.map {
        case (
            Seq(
              barcode,
              variation,
              sku,
              name,
              price,
              discountPrice,
              qty,
              brand,
              category,
              taxRate
            ),
            _
            ) =>
          ExcelProductRow(
            barcode,
            Option(variation).filter(_.nonEmpty),
            sku,
            name,
            Currency.from(price),
            Currency.from(discountPrice),
            qty.toInt,
            Option(brand).filter(_.nonEmpty),
            Option(category).filter(_.nonEmpty),
            Option(taxRate).filter(_.nonEmpty).map(_.toInt)
          )
      }
      
    def parseStockOrderRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelStockOrderRow] =
      rows.map {
        case (
            Seq(
              name,
              sku,
              variation,
              barcode,
              qty,
              price,
              discountPrice,
              category,
              brand,
              taxRate
            ),
            _
            ) =>
          ExcelStockOrderRow(
            name,
            sku,
            Option(variation).filter(_.nonEmpty),
            barcode,
            qty.toInt,
            Currency.from(price),
            Currency.from(discountPrice),
            Option(category).filter(_.nonEmpty),
            Option(brand).filter(_.nonEmpty),
            Option(taxRate).filter(_.nonEmpty).map(_.toInt)
          )
      }

    def parseSaleRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelSaleRow] =
      rows.map {
        case (Seq(barcode, qty), _) => (barcode, Try(qty.toInt).getOrElse(1))
      }.groupBy(_._1)
        .map {
          case (b, r) => (b, r.foldLeft(0)((sum, r) => sum + r._2))
        }
        .map {
          case (b, q) => ExcelSaleRow(b, q)
        }
        .toSeq
  }
}
