package com.merit.modules.excel
import com.merit.modules.products.ProductRow
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID
import com.merit.modules.brands.BrandRow
import com.merit.modules.products.Currency
import org.joda.time.DateTime
import com.merit.modules.sales.SaleStatus
import com.merit.modules.sales.PaymentMethod

sealed trait ExcelRow

case class ExcelProductRow(
  barcode: String,
  variation: Option[String],
  sku: String,
  name: String,
  price: Currency,
  discountPrice: Option[Currency],
  qty: Int,
  brand: Option[String],
  category: Option[String],
  taxRate: Option[Int]
) extends ExcelRow

case class ExcelWebSaleRow(
  orderNo: String,
  total: Currency,
  discount: Currency,
  paymentMethod: PaymentMethod.Value,
  createdAt: DateTime,
  status: SaleStatus.Value,
  productName: String,
  sku: Option[String],
  brand: String,
  barcode: Option[String],
  qty: Int,
  price: Currency,
  tax: Int
)

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
  price: Currency,
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
case class ExcelInventoryCountRow(
  name: String,
  sku: String,
  variation: Option[String],
  barcode: String,
  qty: Int,
  price: Currency,
  discountPrice: Option[Currency],
  category: Option[String],
  brand: Option[String],
  taxRate: Option[Int]
) extends ExcelRow

object ExcelInventoryCountRow {
  def toProductRow(
    row: ExcelInventoryCountRow,
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

object ValidationErrorTypes extends Enumeration {
  type ErrorType = Value
  val DuplicateBarcodeError, EmptyBarcodeError, EmptySkuError, EmptyQtyError,
    InvalidBarcodeError, InvalidQtyError, InvalidPriceError, InvalidTaxRateError,
    InvalidOrderNoError, InvalidDateError, InvalidStatusError = Value
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
    case InvalidOrderNoError   => invalidOrderNo
    case InvalidDateError      => invalidDate
    case InvalidStatusError    => invalidStatus
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
  val invalidOrderNo   = "Invalid order number"
  val invalidDate      = "Invalid date"
  val invalidStatus    = "Invalid sale status"

  val invalidProductImportMessage    = "Product import contains invalid rows"
  val invalidSaleImportMessage       = "Sale import contains invalid rows"
  val invalidStockOrderImportMessage = "Stock order import contains invalid rows"
  val invalidWebSalesImportMessage   = "Web sales import contains invalid rows"
}
