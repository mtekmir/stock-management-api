package com.merit.modules.excel

import collection.JavaConverters._
import java.io.File
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.DataFormatter
import java.text.NumberFormat
import cats.implicits._
import scala.util.Try
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.CellType
import ValidationErrorTypes._
import ExcelErrorMessages._
import com.merit.modules.products.Currency

object FileFor extends Enumeration {
  type FileFor = Value
  val Product, Sale, StockOrder = Value
}

trait ExcelService {
  // def processFile(file: File, fileFor: FileFor.Value): (Seq[String], Seq[Seq[String]])
  def parseProductImportFile(file: File): Either[ExcelError, Seq[ExcelProductRow]]
  def parseSaleImportFile(file: File): Either[ExcelError, Seq[ExcelSaleRow]]
  def parseStockOrderImportFile(file: File): Either[ExcelError, Seq[ExcelStockOrderRow]]
}

object ExcelService {
  val inputLocation = "src/main/resources"
  val numberFormat  = NumberFormat.getInstance()

  def apply() = new ExcelService {
    private def processFile(
      file: File,
      fileFor: FileFor.Value
    ): (Seq[String], Seq[(Seq[String], Int)]) = {
      val wb        = WorkbookFactory.create(file)
      val formatter = new DataFormatter()

      def parseBarcode(c: Cell): String =
        if (formatter.formatCellValue(c).isEmpty) ""
        else
          Try(c.getNumericCellValue().toLong.toString)
            .getOrElse(formatter.formatCellValue(c))

      val (columnCount, barcodeColumn) = fileFor match {
        case FileFor.Product    => (8, 0)
        case FileFor.Sale       => (2, 0)
        case FileFor.StockOrder => (8, 3)
      }

      def rowToCells(sheet: Sheet)(rowNum: Int) = {
        val row = sheet.getRow(rowNum)
        if (row == null) List()
        else
          (0 until columnCount).map(i => (i, row.getCell(i))).map {
            case (_, c) if c == null => ""
            case (i, c) if i == barcodeColumn =>
              parseBarcode(c)
            case (_, c) =>
              formatter.formatCellValue(c)
          }
      }

      val sheet = wb.getSheetAt(0)
      val rows = (for (rowNum <- (sheet.getFirstRowNum() to sheet.getLastRowNum()))
        yield rowToCells(sheet)(rowNum)).zipWithIndex

      val headers = rows.head._1
      (
        headers,
        rows.tail.map(r => (r._1, r._2 + 1)).filterNot(r => r._1.forall(_.isEmpty))
      )
    }

    private def combineValidationErrors(
      errors: Seq[ExcelValidationError]
    ): Seq[ExcelValidationError] =
      errors
        .foldLeft(Map[ValidationErrorTypes.Value, ExcelValidationError]()) {
          case (m, e @ ExcelValidationError(rows, t)) =>
            m + m
              .get(t)
              .map(err => (t -> err.copy(rows = (err.rows ++ rows).distinct)))
              .getOrElse((t, e))
        }
        .map(_._2)
        .toSeq

    def parseProductImportFile(file: File): Either[ExcelError, Seq[ExcelProductRow]] = {

      val (_, rows) = processFile(file, FileFor.Product)
      val errors = rows.collect {
        case (Seq(barcode, _, _, _, _, _, _, _), index) if barcode.isEmpty =>
          ExcelValidationError(Seq(index), EmptyBarcodeError)
        case (Seq(barcode, _, _, _, _, _, _, _), index)
            if Try(barcode.toLong).isFailure || barcode.length <= 6 || barcode.length >= 14 =>
          ExcelValidationError(Seq(index), InvalidBarcodeError)
        case (Seq(_, _, sku, _, _, _, _, _), index) if sku.isEmpty =>
          ExcelValidationError(Seq(index), EmptySkuError)
        case (Seq(_, _, _, _, _, qty, _, _), index) if qty.isEmpty =>
          ExcelValidationError(Seq(index), EmptyQtyError)
        case (Seq(_, _, _, _, _, qty, _, _), index) if !qty.forall(_.isDigit) =>
          ExcelValidationError(Seq(index), InvalidQtyError)
        case (Seq(_, _, _, _, price, _, _, _), index)
            if !price.isEmpty && !Currency.isValid(price) =>
          ExcelValidationError(Seq(index), InvalidPriceError)
      }

      val barcodes       = rows.map(_._1.head)
      val barcodeIndexes =
        // zip rows with row numbers for better error reporting
        barcodes.zipWithIndex.map(r => (r._1, r._2 + 2))
      val duplicates = barcodes
        .diff(barcodes.distinct)
        .filterNot(_.isEmpty)
        .map(
          b =>
            ExcelValidationError(
              barcodeIndexes.filter(_._1 == b).map(_._2),
              DuplicateBarcodeError
            )
        )

      errors ++ duplicates match {
        case Seq() =>
          rows.map {
            case (Seq(barcode, variation, sku, name, price, qty, brand, category), _) =>
              ExcelProductRow(
                barcode,
                Option(variation).filter(_.nonEmpty),
                sku,
                name,
                Currency.from(price),
                qty.toInt,
                Option(brand).filter(_.nonEmpty),
                Option(category).filter(_.nonEmpty)
              )
          }.asRight
        case _ =>
          ExcelError(
            invalidProductImportMessage,
            combineValidationErrors(errors ++ duplicates)
          ).asLeft
      }
    }

    def parseSaleImportFile(file: File): Either[ExcelError, Seq[ExcelSaleRow]] = {
      val (_, rows) = processFile(file, FileFor.Sale)
      val errors = rows.collect {
        case (Seq(barcode, _), index) if barcode.isEmpty =>
          ExcelValidationError(Seq(index), EmptyBarcodeError)
        case (Seq(_, qty), index) if !qty.isEmpty && Try(qty.toInt).isFailure =>
          ExcelValidationError(Seq(index), InvalidQtyError)
      }

      errors match {
        case Seq() =>
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
            .asRight
        case _ => ExcelError(invalidSaleImportMessage, errors).asLeft
      }
    }

    def parseStockOrderImportFile(
      file: File
    ): Either[ExcelError, Seq[ExcelStockOrderRow]] = {
      val (_, rows) = processFile(file, FileFor.StockOrder)

      val errors = rows.collect {
        case (Seq(_, _, _, barcode, _, _, _, _), index) if barcode.isEmpty =>
          ExcelValidationError(Seq(index), EmptyBarcodeError)
        case (Seq(_, _, _, barcode, _, _, _, _), index)
            if !barcode
              .forall(_.isDigit) || barcode.length <= 6 || barcode.length >= 14 =>
          ExcelValidationError(Seq(index), InvalidBarcodeError)
        case (Seq(_, _, _, _, qty, _, _, _), index) if qty.isEmpty =>
          ExcelValidationError(Seq(index), EmptyQtyError)
        case (Seq(_, _, _, _, qty, _, _, _), index) if !qty.forall(_.isDigit) =>
          ExcelValidationError(Seq(index), InvalidQtyError)
        case (Seq(_, _, _, _, _, price, _, _), index)
            if !price.isEmpty && !Currency.isValid(price) =>
          ExcelValidationError(Seq(index), InvalidPriceError)
      }

      errors match {
        case Seq() =>
          rows.map {
            case (Seq(name, sku, variation, barcode, qty, price, category, brand), _) =>
              ExcelStockOrderRow(
                name,
                sku,
                Option(variation).filter(_.nonEmpty),
                barcode,
                qty.toInt,
                Currency.from(price),
                Option(category).filter(_.nonEmpty),
                Option(brand).filter(_.nonEmpty)
              )
          }.asRight
        case es =>
          ExcelError(invalidStockOrderImportMessage, combineValidationErrors(es)).asLeft
      }
    }
  }

}
