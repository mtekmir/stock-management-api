package com.merit.modules.excel
import com.merit.modules.products.Currency
import scala.util.Try
import org.joda.time.format.DateTimeFormat
import com.merit.modules.sales.SaleStatus

trait ExcelServiceValidation {
  def validateProductRows(rows: Vector[(Vector[String], Int)]): Vector[ExcelValidationError]
  def validateSaleRows(rows: Vector[(Vector[String], Int)]): Vector[ExcelValidationError]
  def validateStockOrderRows(rows: Vector[(Vector[String], Int)]): Vector[ExcelValidationError]
  def validateWebSaleRows(rows: Vector[(Vector[String], Int)]): Vector[ExcelValidationError]
  def combineValidationErrors(errors: Vector[ExcelValidationError]): Vector[ExcelValidationError]
}

object ExcelServiceValidation {
  def apply(): ExcelServiceValidation = new ExcelServiceValidation {
    import ValidationErrorTypes._
    import ExcelErrorMessages._

    def combineValidationErrors(
      errors: Vector[ExcelValidationError]
    ): Vector[ExcelValidationError] =
      errors
        .foldLeft(Map[ValidationErrorTypes.Value, ExcelValidationError]()) {
          case (m, e @ ExcelValidationError(rows, t)) =>
            m + m
              .get(t)
              .map(err => (t -> err.copy(rows = (err.rows ++ rows).distinct)))
              .getOrElse((t, e))
        }
        .map(_._2)
        .toVector

    def validateProductRows(rows: Vector[(Vector[String], Int)]): Vector[ExcelValidationError] = {
      val validationErrors =
        rows.collect {
          case (Vector(barcode, _, _, _, _, _, _, _, _, _), index) if barcode.isEmpty =>
            ExcelValidationError(Vector(index), EmptyBarcodeError)
          case (Vector(barcode, _, _, _, _, _, _, _, _, _), index)
              if Try(barcode.toLong).isFailure || barcode.length <= 6 || barcode.length > 14 =>
            ExcelValidationError(Vector(index), InvalidBarcodeError)
          case (Vector(_, _, sku, _, _, _, _, _, _, _), index) if sku.isEmpty =>
            ExcelValidationError(Vector(index), EmptySkuError)
          case (Vector(_, _, _, _, _, _, qty, _, _, _), index) if qty.isEmpty =>
            ExcelValidationError(Vector(index), EmptyQtyError)
          case (Vector(_, _, _, _, _, _, qty, _, _, _), index) if !qty.forall(_.isDigit) =>
            ExcelValidationError(Vector(index), InvalidQtyError)
          case (Vector(_, _, _, _, price, _, _, _, _, _), index)
              if !price.isEmpty && !Currency.isValid(price) =>
            ExcelValidationError(Vector(index), InvalidPriceError)
          case (Vector(_, _, _, _, _, discountPrice, _, _, _, _), index)
              if !discountPrice.isEmpty && !Currency.isValid(discountPrice) =>
            ExcelValidationError(Vector(index), InvalidPriceError)
          case (Vector(_, _, _, _, _, _, _, _, _, tax), index) if !tax.forall(_.isDigit) =>
            ExcelValidationError(Vector(index), InvalidTaxRateError)
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

      validationErrors ++ duplicates
    }

    def validateSaleRows(rows: Vector[(Vector[String], Int)]): Vector[ExcelValidationError] =
      rows.collect {
        case (Vector(barcode, _), index) if barcode.isEmpty =>
          ExcelValidationError(Vector(index), EmptyBarcodeError)
        case (Vector(_, qty), index) if !qty.isEmpty && Try(qty.toInt).isFailure =>
          ExcelValidationError(Vector(index), InvalidQtyError)
      }

    def validateStockOrderRows(rows: Vector[(Vector[String], Int)]): Vector[ExcelValidationError] =
      rows.collect {
        case (Vector(_, _, _, barcode, _, _, _, _, _, _), index) if barcode.isEmpty =>
          ExcelValidationError(Vector(index), EmptyBarcodeError)
        case (Vector(_, _, _, barcode, _, _, _, _, _, _), index)
            if !barcode
              .forall(_.isDigit) || barcode.length <= 6 || barcode.length > 14 =>
          ExcelValidationError(Vector(index), InvalidBarcodeError)
        case (Vector(_, _, _, _, qty, _, _, _, _, _), index) if qty.isEmpty =>
          ExcelValidationError(Vector(index), EmptyQtyError)
        case (Vector(_, _, _, _, qty, _, _, _, _, _), index) if !qty.forall(_.isDigit) =>
          ExcelValidationError(Vector(index), InvalidQtyError)
        case (Vector(_, _, _, _, _, price, _, _, _, _), index)
            if !price.isEmpty && !Currency.isValid(price) =>
          ExcelValidationError(Vector(index), InvalidPriceError)
        case (Vector(_, _, _, _, _, _, discountPrice, _, _, _), index)
            if !discountPrice.isEmpty && !Currency.isValid(discountPrice) =>
          ExcelValidationError(Vector(index), InvalidPriceError)
        case (Vector(_, _, _, _, _, _, _, _, _, tax), index) if !tax.forall(_.isDigit) =>
          ExcelValidationError(Vector(index), InvalidTaxRateError)
      }

    def validateWebSaleRows(rows: Vector[(Vector[String], Int)]): Vector[ExcelValidationError] = {
      val formatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
      rows.collect {
        case (row, index)
            if !Currency.isValid(row(10)) =>
          ExcelValidationError(Vector(index), InvalidPriceError)
        case (row, index)
            if !row(11).isEmpty && !Currency.isValid(row(11)) =>
          ExcelValidationError(Vector(index), InvalidPriceError)
        case (row, index)
            if Try(formatter.parseDateTime(row(17))).isFailure =>
          ExcelValidationError(Vector(index), InvalidDateError)
        case (row, index)
            if !SaleStatus.isValid(row(18)) =>
          ExcelValidationError(Vector(index), InvalidStatusError)
        case (row, index)
            if !row(50).isEmpty && (!row(50)
              .forall(_.isDigit) || row(50).length <= 6 || row(50).length > 14) =>
          ExcelValidationError(Vector(index), InvalidBarcodeError)
        case (row, index) if row(52).isEmpty =>
          ExcelValidationError(Vector(index), EmptyQtyError)
        case (row, index) if !row(52).forall(_.isDigit) =>
          ExcelValidationError(Vector(index), InvalidQtyError)
        case (row, index)
            if !row(54).isEmpty && !Currency.isValid(row(54)) =>
          ExcelValidationError(Vector(index), InvalidPriceError)
        case (row, index) if !row(56).forall(_.isDigit) =>
          ExcelValidationError(Vector(index), InvalidTaxRateError)
      }
    }
  }
}
