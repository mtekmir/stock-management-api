package com.merit.modules.excel
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID
import com.merit.modules.brands.BrandRow
import com.merit.modules.products.Currency

sealed trait ExcelRow

case class ExcelProductRow(
  barcode: String,
  variation: Option[String],
  sku: String,
  name: String,
  price: Option[Currency],
  discountPrice: Option[Currency],
  qty: Int,
  brand: Option[String],
  category: Option[String],
  taxRate: Option[Int]
) extends ExcelRow

object ExcelProductRow {
  def toProductRow(
    row: ExcelProductRow,
    brandId: Option[BrandID],
    categoryId: Option[CategoryID]
  ): ProductRow = {
    import row._
    ProductRow(
      barcode,
      sku,
      name,
      price,
      discountPrice,
      qty,
      variation,
      taxRate,
      brandId,
      categoryId
    )
  }
}

case class ExcelSaleRow(
  barcode: String,
  qty: Int = 1
) extends ExcelRow

case class ExcelStockOrderRow(
  name: String,
  sku: String,
  variation: Option[String],
  barcode: String,
  qty: Int,
  price: Option[Currency],
  discountPrice: Option[Currency],
  category: Option[String],
  brand: Option[String],
  taxRate: Option[Int]
) extends ExcelRow

object ExcelStockOrderRow {
  def toProductRow(
    row: ExcelStockOrderRow,
    brandId: Option[BrandID],
    categoryId: Option[CategoryID]
  ): ProductRow = {
    import row._

    ProductRow(barcode, sku, name, price, discountPrice, qty, variation, taxRate, brandId, categoryId)
  }
}

object ValidationErrorTypes extends Enumeration {
  type ErrorType = Value
  val DuplicateBarcodeError, EmptyBarcodeError, EmptySkuError, EmptyQtyError,
    InvalidBarcodeError, InvalidQtyError, InvalidPriceError, InvalidTaxRateError = Value
}

final case class ExcelError(
  message: String,
  validationErrors: Seq[ExcelValidationError]
)

case class ExcelValidationError(
  rows: Seq[Int],
  errorType: ValidationErrorTypes.Value
) {
  import ExcelErrorMessages._
  import ValidationErrorTypes._
  val message = errorType match {
    case DuplicateBarcodeError => duplicateBarcode
    case EmptyBarcodeError     => emptyBarcode
    case EmptyQtyError         => emptyQty
    case EmptySkuError         => emptySku
    case InvalidBarcodeError   => invalidBarcode
    case InvalidQtyError       => invalidQty
    case InvalidPriceError     => invalidPrice
    case InvalidTaxRateError   => invalidTaxRate
  }
}

object ExcelErrorMessages {
  val emptyBarcode     = "Barcode must not be empty"
  val emptySku         = "Sku must not be empty"
  val emptyQty         = "Quantity must not be empty"
  val duplicateBarcode = "Duplicate barcodes"
  val invalidBarcode   = "Barcode is invalid"
  val invalidQty       = "Quantity is not a number"
  val invalidPrice     = "Invalid price value"
  val invalidTaxRate   = "Invalid tax rate"

  val invalidProductImportMessage    = "Product import contains invalid rows"
  val invalidSaleImportMessage       = "Sale import contains invalid rows"
  val invalidStockOrderImportMessage = "Stock order import contains invalid rows"
}
