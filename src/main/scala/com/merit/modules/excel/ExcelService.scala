package com.merit.modules.excel

import collection.JavaConverters._
import java.io.File
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.DataFormatter
import java.text.NumberFormat

object FileFor extends Enumeration {
  type FileFor = Value
  val product, sale, stockOrder = Value
}

trait ExcelService {
  def processFile(file: File, fileFor: FileFor.Value): (Seq[String], Seq[Seq[String]])
  def parseProductImportFile(file: File): Seq[ExcelProductRow]
  def parseSaleImportFile(file: File): Seq[ExcelSaleRow]
  def parseStockOrderImportFile(file: File): Seq[ExcelStockOrderRow]
  def validateProductRows(rows: Seq[ExcelProductRow]): Seq[DuplicateBarcode]
}

object ExcelService {
  val inputLocation = "src/main/resources"
  val numberFormat  = NumberFormat.getInstance()

  def apply() = new ExcelService {
    def processFile(
      file: File,
      fileFor: FileFor.Value
    ): (Seq[String], Seq[Seq[String]]) = {
      val wb        = WorkbookFactory.create(file)
      val formatter = new DataFormatter()

      val columnCount = fileFor match {
        case FileFor.product    => 8
        case FileFor.stockOrder => 8
        case FileFor.sale       => 2
      }

      val cells = (for {
        data <- wb.getSheetAt(0).rowIterator().asScala
        cell <- data.cellIterator().asScala
      } yield formatter.formatCellValue(cell))

      val rows = cells.toSeq.sliding(columnCount, columnCount).toSeq

      val headers = rows.head
      (headers, rows.tail)
    }

    def parseProductImportFile(file: File): Seq[ExcelProductRow] = {
      val (_, rows) = processFile(file, FileFor.product)
      rows.map {
        case Seq(barcode, variation, sku, name, price, qty, brand, category) =>
          ExcelProductRow(
            barcode,
            Option(variation),
            sku,
            name,
            numberFormat.parse(price).doubleValue(),
            qty.toInt,
            Option(brand),
            Option(category)
          )
      }
    }

    def parseSaleImportFile(file: File): Seq[ExcelSaleRow] = {
      val (_, rows) = processFile(file, FileFor.sale)
      rows.map {
        case Seq(barcode, qty) => ExcelSaleRow(barcode, qty.toInt)
      }
    }

    def parseStockOrderImportFile(file: File): Seq[ExcelStockOrderRow] = {
      val (_, rows) = processFile(file, FileFor.stockOrder)

      rows.map {
        case Seq(name, sku, variation, barcode, qty, price, category, brand) =>
          ExcelStockOrderRow(
            name,
            sku,
            Option(variation),
            barcode,
            qty.toInt,
            numberFormat.parse(price).doubleValue(),
            Option(category),
            Option(brand)
          )
      }
    }

    def validateProductRows(rows: Seq[ExcelProductRow]): Seq[DuplicateBarcode] = {
      val barcodes       = rows.map(_.barcode)
      val barcodeIndexes = barcodes.zipWithIndex
      barcodes
        .diff(barcodes.distinct)
        .map(b => DuplicateBarcode(b, barcodeIndexes.filter(_._1 == b).map(_._2 + 1)))
    }
  }

}
