package com.merit.modules.sales

import slick.lifted.MappedTo
import java.sql.Timestamp
import com.merit.modules.products._
import org.joda.time.DateTime
import com.merit.modules.brands.BrandRow
import com.merit.modules.categories.CategoryRow

case class SaleID(value: Long) extends AnyVal with MappedTo[Long]

case class SaleRow(
  createdAt: DateTime = DateTime.now(),
  total: Currency,
  id: SaleID = SaleID(0L)
)

case class SaleDTO(
  id: SaleID,
  createdAt: DateTime,
  total: Currency,
  products: Seq[SaleDTOProduct]
)

case class SaleDTOProduct(
  id: ProductID,
  barcode: String,
  sku: String,
  name: String,
  price: Option[Currency],
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
    synced: Boolean
  ): SaleDTOProduct = {
    import productRow._
    SaleDTOProduct(
      id,
      barcode,
      sku,
      name,
      price,
      discountPrice,
      qty,
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
  products: Seq[SaleSummaryProduct]
)
