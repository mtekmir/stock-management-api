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
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream
import com.merit.modules.products.ProductRow
import com.merit.modules.products.ProductDTO
import com.merit.modules.inventoryCount.InventoryCountDTO
import akka.stream.scaladsl.Source
import akka.util.ByteString
import java.io.OutputStream
import scala.concurrent.Future
import akka.stream.IOResult

object FileFor extends Enumeration {
  type FileFor = Value
  val Product, Sale, StockOrder = Value
}

trait ExcelService {
  def parseProductImportFile(file: File): Either[ExcelError, Seq[ExcelProductRow]]
  def parseSaleImportFile(file: File): Either[ExcelError, Seq[ExcelSaleRow]]
  def parseStockOrderImportFile(file: File): Either[ExcelError, Seq[ExcelStockOrderRow]]
  // def writeStockOrderRows(name: String, rows: Seq[ExcelStockOrderRow]): File
  // def writeProductRows(name: String, rows: Seq[ProductDTO]): File
  // def writeInventoryCountBatch(data: InventoryCountDTO): Source[ByteString, Future[IOResult]]
}

object ExcelService {
  val inputLocation = "src/main/resources"
  val numberFormat  = NumberFormat.getInstance()
  private val Validator     = ExcelServiceValidation()
  private val Parser        = ExcelParser()
  private val Writer        = ExcelWriter()

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
        case FileFor.Product    => (10, 0)
        case FileFor.Sale       => (2, 0)
        case FileFor.StockOrder => (10, 3)
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

    def parseProductImportFile(file: File): Either[ExcelError, Seq[ExcelProductRow]] = {
      val (_, rows) = processFile(file, FileFor.Product)

      val errors = Validator.validateProductRows(rows)

      errors match {
        case Seq() => Parser.parseProductRows(rows).asRight
        case _ =>
          ExcelError(
            invalidProductImportMessage,
            Validator.combineValidationErrors(errors)
          ).asLeft
      }
    }

    def parseSaleImportFile(file: File): Either[ExcelError, Seq[ExcelSaleRow]] = {
      val (_, rows) = processFile(file, FileFor.Sale)
      val errors    = Validator.validateSaleRows(rows)

      errors match {
        case Seq() => Parser.parseSaleRows(rows).asRight
        case _ =>
          ExcelError(invalidSaleImportMessage, Validator.combineValidationErrors(errors)).asLeft
      }
    }

    def parseStockOrderImportFile(
      file: File
    ): Either[ExcelError, Seq[ExcelStockOrderRow]] = {
      val (_, rows) = processFile(file, FileFor.StockOrder)

      val errors = Validator.validateStockOrderRows(rows)

      errors match {
        case Seq() => Parser.parseStockOrderRows(rows).asRight
        case es =>
          ExcelError(invalidStockOrderImportMessage, Validator.combineValidationErrors(es)).asLeft
      }
    }

    // def writeStockOrderRows(name: String, rows: Seq[ExcelStockOrderRow]) = 
    //   Writer.writeStockOrderRows(name, rows)
      
    // def writeProductRows(name: String, rows: Seq[ProductDTO]) =
    //   Writer.writeProductRows(name, rows)

    // def writeInventoryCountBatch(data: InventoryCountDTO): Source[ByteString, Future[IOResult]] = 
    //   Writer.writeInventoryCountBatch(data)
  }
}
