package com.merit.modules.excel

import collection.JavaConverters._
import java.io.File
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.DataFormatter
import java.text.NumberFormat

trait ExcelService {
  def parseProductImportFile(file: File): Vector[ExcelProductRow]
  def parseSaleImportFile(file: File): Vector[ExcelSaleRow]
  def validateProductRows(rows: Vector[ExcelProductRow]): Vector[DuplicateBarcode]
}

object ExcelService {
  val inputLocation = "src/main/resources"
  val numberFormat  = NumberFormat.getInstance()

  def apply() = new ExcelService {
    private def processFile(file: File): (Vector[String], Vector[Vector[String]]) = {
      val wb        = WorkbookFactory.create(file)
      val sheet     = wb.getSheetAt(0).rowIterator().asScala
      val formatter = new DataFormatter()

      val data = (for {
        row <- sheet
      } yield row.cellIterator().asScala.toVector.map(formatter.formatCellValue)).toVector

      val headers = data.head
      (headers, data.tail)
    }

    def parseProductImportFile(file: File): Vector[ExcelProductRow] = {
      val (_, rows) = processFile(file)
      rows.map {
        case Vector(barcode, variation, sku, name, price, qty, brand) =>
          ExcelProductRow(
            barcode,
            variation,
            sku,
            name,
            numberFormat.parse(price).doubleValue(),
            qty.toInt,
            brand
          )
      }
    }

    def parseSaleImportFile(file: File): Vector[ExcelSaleRow] = {
      val (_, rows) = processFile(file)
      rows.map {
        case Vector(barcode, qty) => ExcelSaleRow(barcode, qty.toInt)
      }
    }

    def validateProductRows(rows: Vector[ExcelProductRow]): Vector[DuplicateBarcode] = {
      val barcodes       = rows.map(_.barcode)
      val barcodeIndexes = barcodes.zipWithIndex
      barcodes
        .diff(barcodes.distinct)
        .map(b => DuplicateBarcode(b, barcodeIndexes.filter(_._1 == b).map(_._2 + 1)))
    }
  }

}
