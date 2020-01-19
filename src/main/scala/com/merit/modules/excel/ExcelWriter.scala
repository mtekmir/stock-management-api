package com.merit.modules.excel

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream
import com.merit.modules.products.Currency
import com.merit.modules.products.ProductDTO
import com.merit.modules.products.ProductID
import java.io.File
import org.apache.poi.xssf.usermodel.XSSFSheet
import com.merit.modules.inventoryCount.InventoryCountDTO
import akka.stream.scaladsl.FileIO
import java.nio.file.Paths
import akka.stream.scaladsl.Source
import akka.util.ByteString
import scala.concurrent.Future
import akka.stream.IOResult
import java.io.OutputStream
import org.joda.time.format.DateTimeFormat

trait ExcelWriter {
  def writeStockOrderRows(
    name: String,
    rows: Seq[ExcelStockOrderRow]
  ): Source[ByteString, Future[IOResult]]
  def writeProductRows(
    name: String,
    rows: Seq[ProductDTO]
  ): Source[ByteString, Future[IOResult]]
  def writeInventoryCountBatch(data: InventoryCountDTO): Source[ByteString, Future[IOResult]]
}

object ExcelWriter {
  def apply(): ExcelWriter = new ExcelWriter {
    val outputLocation = "src/main/resources/excelOutput"
    val dateFormatter  = DateTimeFormat.forPattern("d/MM/yyyy");
    private def decodeCurrency(c: Option[Currency]) =
      c match {
        case None           => ""
        case Some(currency) => currency.value.toString
      }

    private def decodeOption[A](o: Option[A]): String =
      o.map(_.toString).getOrElse("")

    private def writeToFile[R <: { def productIterator: Iterator[Any] }](
      wb: XSSFWorkbook,
      sheet: XSSFSheet,
      name: String,
      headers: Seq[String],
      rows: Seq[R]
    ): Unit = {
      val headerRow = sheet.createRow(0)
      headers.zipWithIndex.foreach {
        case (h, i) =>
          headerRow.createCell(i).setCellValue(h)
      }

      rows.zipWithIndex.foreach {
        case (row, rowIdx) => {
          val sheetRow = sheet.createRow(rowIdx + 1)

          row.productIterator.zipWithIndex.foreach {
            case (c, cellIdx) =>
              c match {
                case ProductID(value) =>
                  sheetRow.createCell(cellIdx).setCellValue(value.toString)
                case value: String => sheetRow.createCell(cellIdx).setCellValue(value)
                case value: Int    => sheetRow.createCell(cellIdx).setCellValue(value)
                case Some(Currency(value)) =>
                  sheetRow.createCell(cellIdx).setCellValue(value.toString)
                case Some(value) => sheetRow.createCell(cellIdx).setCellValue(value.toString)
                case _           => sheetRow.createCell(cellIdx).setCellValue("")
              }
          }
        }
      }

      val fileOut = new FileOutputStream(s"$outputLocation/$name.xlsx")
      wb.write(fileOut)
    }

    def writeStockOrderRows(
      name: String,
      rows: Seq[ExcelStockOrderRow]
    ): Source[ByteString, Future[IOResult]] = {
      val headers = List(
        "name",
        "sku",
        "variation",
        "barcode",
        "qty",
        "price",
        "discountPrice",
        "category",
        "brand",
        "taxRate"
      )

      val wb    = new XSSFWorkbook()
      val sheet = wb.createSheet("Sheet 1")
      writeToFile(wb, sheet, name, headers, rows)
      
      FileIO.fromPath(Paths.get(s"$outputLocation/$name.xlsx"))
    }

    def writeProductRows(
      name: String,
      rows: Seq[ProductDTO]
    ): Source[ByteString, Future[IOResult]] = {
      val headers = List(
        "id",
        "barcode",
        "sku",
        "name",
        "price",
        "discountPrice",
        "qty",
        "variation",
        "taxRate",
        "brand",
        "category"
      )

      val wb    = new XSSFWorkbook()
      val sheet = wb.createSheet("Sheet 1")
      writeToFile(wb, sheet, name, headers, rows)
      FileIO.fromPath(Paths.get(s"$outputLocation/$name.xlsx"))
    }

    def writeInventoryCountBatch(
      data: InventoryCountDTO
    ): Source[ByteString, Future[IOResult]] = {
      val wb    = new XSSFWorkbook()
      val sheet = wb.createSheet("Sheet 1")
      import data._

      println(started)
      val firstRow = sheet.createRow(1)
      firstRow.createCell(0).setCellValue("Started:")
      firstRow.createCell(1).setCellValue(dateFormatter.print(started))
      val secondRow = sheet.createRow(2)
      secondRow.createCell(0).setCellValue("Name:")
      secondRow.createCell(1).setCellValue(name.getOrElse(""))
      val thirdRow = sheet.createRow(3)
      thirdRow.createCell(0).setCellValue("Category:")
      thirdRow.createCell(1).setCellValue(category.getOrElse(""))
      val fourthRow = sheet.createRow(4)
      thirdRow.createCell(0).setCellValue("Brand:")
      thirdRow.createCell(1).setCellValue(brand.getOrElse(""))

      val headers = Seq(
        "sku",
        "barcode",
        "name",
        "variation",
        "expected",
        "counted"
      )
      val fileName = s"InventoryCount_${dateFormatter.print(started)}"
      writeToFile(wb, sheet, fileName, headers, products.map(_.toExcelRow))

      FileIO.fromPath(Paths.get(s"$outputLocation/$fileName.xlsx"))
    }
  }
}
