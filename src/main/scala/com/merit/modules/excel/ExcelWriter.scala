package com.merit.modules.excel

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream
import com.merit.modules.products.Currency
import com.merit.modules.products.ProductDTO
import com.merit.modules.products.ProductID

trait ExcelWriter {
  def writeStockOrderRows(name: String, rows: Seq[ExcelStockOrderRow]): Unit
  def writeProductRows(name: String, rows: Seq[ProductDTO]): Unit
}

object ExcelWriter {
  def apply(): ExcelWriter = new ExcelWriter {
    val outputLocation = "src/main/resources"
    private def decodeCurrency(c: Option[Currency]) =
      c match {
        case None           => ""
        case Some(currency) => currency.value.toString
      }

    private def decodeOption[A](o: Option[A]): String =
      o.map(_.toString).getOrElse("")

    private def writeToFile[R <: { def productIterator: Iterator[Any] }](
      name: String,
      headers: Seq[String],
      rows: Seq[R]
    ): Unit = {
      val wb    = new XSSFWorkbook()
      val sheet = wb.createSheet("Sheet 1")

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

    def writeStockOrderRows(name: String, rows: Seq[ExcelStockOrderRow]): Unit = {
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

      writeToFile(name, headers, rows)
    }

    def writeProductRows(name: String, rows: Seq[ProductDTO]): Unit = {
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

      writeToFile(name, headers, rows)
    }
  }
}
