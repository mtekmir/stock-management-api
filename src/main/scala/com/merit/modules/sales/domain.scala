package com.merit.modules.sales

import slick.lifted.MappedTo
import java.sql.Timestamp
import com.merit.modules.products._
import org.joda.time.DateTime

case class SaleID(value: Long) extends AnyVal with MappedTo[Long]

case class SaleRow(
  createdAt: DateTime = DateTime.now(),
  id: SaleID = SaleID(0L)
)

case class SaleDTO(
  id: SaleID,
  createdAt: DateTime,
  products: Seq[ProductDTO]
)

case class SaleSummaryProduct(
  barcode: String,
  name: Option[String] = None,
  variation: Option[String] = None,
  prevQty: Option[Int] = None,
  newQty: Option[Int] = None
)

object SaleSummaryProduct {
  def fromProductRow(p: ProductDTO, soldQty: Int): SaleSummaryProduct = {
    import p._
    SaleSummaryProduct(barcode, Some(name), Some(variation), Some(qty + soldQty), Some(qty))
  } 
}

case class SaleSummary(
  products: Seq[SaleSummaryProduct]
)