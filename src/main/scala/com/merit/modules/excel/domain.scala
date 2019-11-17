package com.merit.modules.excel

sealed trait ExcelRow

case class ExcelProductRow(
  barcode: String,
  variation: String,
  sku: String,
  name: String,
  price: Double,
  qty: Int,
  brand: String
) extends ExcelRow

case class DuplicateBarcode(
  barcode: String,
  rowIndex: Seq[Int]
)

case class ExcelSaleRow(
  barcode: String,
  qty: Int = 1
) extends ExcelRow