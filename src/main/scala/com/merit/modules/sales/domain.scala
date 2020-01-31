package com.merit.modules.sales

import slick.lifted.MappedTo
import java.sql.Timestamp
import com.merit.modules.products._
import org.joda.time.DateTime
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow

case class SaleID(value: Long) extends AnyVal with MappedTo[Long]

object SaleOutlet extends Enumeration {
  type SaleOutlet = Value
  val Store = Value("Store")
  val Web   = Value("Web")
}

case class SaleRow(
  createdAt: DateTime = DateTime.now(),
  total: Currency,
  discount: Currency = Currency(0),
  outlet: SaleOutlet.Value = SaleOutlet.Store,
  id: SaleID = SaleID(0L)
)

case class SaleDTO(
  id: SaleID,
  createdAt: DateTime,
  outlet: SaleOutlet.Value,
  total: Currency,
  discount: Currency,
  products: Seq[SaleDTOProduct]
)

case class SaleDTOProduct(
  id: ProductID,
  barcode: String,
  sku: String,
  name: String,
  price: Currency,
  discountPrice: Option[Currency],
  qty: Int,
  variation: Option[String],
  taxRate: Option[Int],
  brand: Option[String],
  category: Option[String],
  synced: Boolean = false
)

object SaleDTOProduct {
  def fromRow(
    productRow: ProductRow,
    brand: Option[BrandRow] = None,
    category: Option[CategoryRow] = None,
    synced: Boolean,
    soldQty: Int
  ): SaleDTOProduct = {
    import productRow._
    SaleDTOProduct(
      id,
      barcode,
      sku,
      name,
      price,
      discountPrice,
      soldQty,
      variation,
      taxRate,
      brand.map(_.name),
      category.map(_.name),
      synced
    )
  }
}

case class SaleSummaryProduct(
  id: ProductID,
  barcode: String,
  name: String,
  variation: Option[String] = None,
  prevQty: Int,
  soldQty: Int
)

object SaleSummaryProduct {
  def fromProductDTO(p: ProductDTO, soldQty: Int): SaleSummaryProduct = {
    import p._
    SaleSummaryProduct(id, barcode, name, variation, qty, soldQty)
  }
}

case class SaleSummary(
  id: SaleID,
  createdAt: DateTime,
  total: Currency,
  discount: Currency,
  outlet: SaleOutlet.Value,
  products: Seq[SaleSummaryProduct]
)

case class SaleFilters(
  startDate: Option[DateTime] = None,
  endDate: Option[DateTime] = None
)

case class PaginatedSalesResponse(
  count: Int,
  sales: Seq[SaleDTO]
)
