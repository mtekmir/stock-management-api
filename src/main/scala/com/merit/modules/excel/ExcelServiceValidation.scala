package com.merit.modules.excel
import com.merit.modules.products.Currency
import scala.util.Try
import org.joda.time.format.DateTimeFormat
import com.merit.modules.sales.SaleStatus

trait ExcelServiceValidation {
  def validateProductRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelValidationError]
  def validateSaleRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelValidationError]
  def validateStockOrderRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelValidationError]
  def validateWebSaleRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelValidationError]
  def combineValidationErrors(errors: Seq[ExcelValidationError]): Seq[ExcelValidationError]
}

object ExcelServiceValidation {
  def apply(): ExcelServiceValidation = new ExcelServiceValidation {
    import ValidationErrorTypes._
    import ExcelErrorMessages._

    def combineValidationErrors(
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

    def validateProductRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelValidationError] = {
      val validationErrors =
        rows.collect {
          case (Seq(barcode, _, _, _, _, _, _, _, _, _), index) if barcode.isEmpty =>
            ExcelValidationError(Seq(index), EmptyBarcodeError)
          case (Seq(barcode, _, _, _, _, _, _, _, _, _), index)
              if Try(barcode.toLong).isFailure || barcode.length <= 6 || barcode.length > 14 =>
            ExcelValidationError(Seq(index), InvalidBarcodeError)
          case (Seq(_, _, sku, _, _, _, _, _, _, _), index) if sku.isEmpty =>
            ExcelValidationError(Seq(index), EmptySkuError)
          case (Seq(_, _, _, _, _, _, qty, _, _, _), index) if qty.isEmpty =>
            ExcelValidationError(Seq(index), EmptyQtyError)
          case (Seq(_, _, _, _, _, _, qty, _, _, _), index) if !qty.forall(_.isDigit) =>
            ExcelValidationError(Seq(index), InvalidQtyError)
          case (Seq(_, _, _, _, price, _, _, _, _, _), index)
              if !price.isEmpty && !Currency.isValid(price) =>
            ExcelValidationError(Seq(index), InvalidPriceError)
          case (Seq(_, _, _, _, _, discountPrice, _, _, _, _), index)
              if !discountPrice.isEmpty && !Currency.isValid(discountPrice) =>
            ExcelValidationError(Seq(index), InvalidPriceError)
          case (Seq(_, _, _, _, _, _, _, _, _, tax), index) if !tax.forall(_.isDigit) =>
            ExcelValidationError(Seq(index), InvalidTaxRateError)
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

    def validateSaleRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelValidationError] =
      rows.collect {
        case (Seq(barcode, _), index) if barcode.isEmpty =>
          ExcelValidationError(Seq(index), EmptyBarcodeError)
        case (Seq(_, qty), index) if !qty.isEmpty && Try(qty.toInt).isFailure =>
          ExcelValidationError(Seq(index), InvalidQtyError)
      }

    def validateStockOrderRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelValidationError] =
      rows.collect {
        case (Seq(_, _, _, barcode, _, _, _, _, _, _), index) if barcode.isEmpty =>
          ExcelValidationError(Seq(index), EmptyBarcodeError)
        case (Seq(_, _, _, barcode, _, _, _, _, _, _), index)
            if !barcode
              .forall(_.isDigit) || barcode.length <= 6 || barcode.length >= 14 =>
          ExcelValidationError(Seq(index), InvalidBarcodeError)
        case (Seq(_, _, _, _, qty, _, _, _, _, _), index) if qty.isEmpty =>
          ExcelValidationError(Seq(index), EmptyQtyError)
        case (Seq(_, _, _, _, qty, _, _, _, _, _), index) if !qty.forall(_.isDigit) =>
          ExcelValidationError(Seq(index), InvalidQtyError)
        case (Seq(_, _, _, _, _, price, _, _, _, _), index)
            if !price.isEmpty && !Currency.isValid(price) =>
          ExcelValidationError(Seq(index), InvalidPriceError)
        case (Seq(_, _, _, _, _, _, discountPrice, _, _, _), index)
            if !discountPrice.isEmpty && !Currency.isValid(discountPrice) =>
          ExcelValidationError(Seq(index), InvalidPriceError)
        case (Seq(_, _, _, _, _, _, _, _, _, tax), index) if !tax.forall(_.isDigit) =>
          ExcelValidationError(Seq(index), InvalidTaxRateError)
      }

    def validateWebSaleRows(rows: Seq[(Seq[String], Int)]): Seq[ExcelValidationError] = {
      val formatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm")
      rows.collect {
        case (Seq(_, total, _, _, _, _, _, _, _, _, _, _), index)
            if !Currency.isValid(total) =>
          ExcelValidationError(Seq(index), InvalidPriceError)
        case (Seq(_, _, discount, _, _, _, _, _, _, _, _, _), index)
            if !discount.isEmpty && !Currency.isValid(discount) =>
          ExcelValidationError(Seq(index), InvalidPriceError)
        case (Seq(_, _, _, createdAt, _, _, _, _, _, _, _, _), index)
            if Try(formatter.parseDateTime(createdAt)).isFailure =>
          ExcelValidationError(Seq(index), InvalidDateError)
        case (Seq(_, _, _, _, status, _, _, _, _, _, _, _), index)
            if !SaleStatus.isValid(status) =>
          ExcelValidationError(Seq(index), InvalidStatusError)
        case (Seq(_, _, _, _, _, _, _, _, barcode, _, _, _), index)
            if !barcode.isEmpty && (!barcode
              .forall(_.isDigit) || barcode.length <= 6 || barcode.length >= 14) =>
          ExcelValidationError(Seq(index), InvalidBarcodeError)
        case (Seq(_, _, _, _, _, _, _, _, _, qty, _, _), index) if qty.isEmpty =>
          ExcelValidationError(Seq(index), EmptyQtyError)
        case (Seq(_, _, _, _, _, _, _, _, _, qty, _, _), index) if !qty.forall(_.isDigit) =>
          ExcelValidationError(Seq(index), InvalidQtyError)
        case (Seq(_, _, _, _, _, _, _, _, _, _, price, _), index)
            if !price.isEmpty && !Currency.isValid(price) =>
          ExcelValidationError(Seq(index), InvalidPriceError)
        case (Seq(_, _, _, _, _, _, _, _, _, _, _, tax), index) if !tax.forall(_.isDigit) =>
          ExcelValidationError(Seq(index), InvalidTaxRateError)
      }
    }
  }
}
