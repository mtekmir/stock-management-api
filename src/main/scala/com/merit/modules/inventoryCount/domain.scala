package com.merit.modules.inventoryCount

import org.joda.time.DateTime
import slick.lifted.MappedTo
import com.merit.modules.products.{ProductID, ProductDTO, ProductRow}
import com.merit.modules.brands.{BrandRow, BrandID}
import com.merit.modules.categories.{CategoryRow, CategoryID}

case class InventoryCountBatchID(value: Long) extends AnyVal with MappedTo[Long]

sealed trait InventoryCountStatus
object InventoryCountStatus {
  case object Open      extends InventoryCountStatus
  case object Completed extends InventoryCountStatus
  case object Cancelled extends InventoryCountStatus
  def fromString(s: String): InventoryCountStatus =
    s match {
      case "Completed" | "completed" => Completed
      case "Cancelled" | "cancelled" => Cancelled
      case _                         => Open
    }
  def toString(s: InventoryCountStatus) =
    s match {
      case Open      => "Open"
      case Completed => "Completed"
      case Cancelled => "Cancelled"
    }
}

sealed trait InventoryCountProductStatus
object InventoryCountProductStatus {
  case object Counted   extends InventoryCountProductStatus
  case object UnCounted extends InventoryCountProductStatus
  case object All       extends InventoryCountProductStatus
  def fromString(s: String): InventoryCountProductStatus =
    s match {
      case "counted" | "Counted"     => Counted
      case "uncounted" | "uncounted" => UnCounted
      case _                         => All
    }
}

case class InventoryCountBatchRow(
  started: DateTime,
  finished: Option[DateTime],
  name: Option[String],
  categoryId: Option[CategoryID],
  brandId: Option[BrandID],
  status: InventoryCountStatus = InventoryCountStatus.Open,
  id: InventoryCountBatchID = InventoryCountBatchID(0)
)

case class InventoryCountProductID(value: Long) extends AnyVal with MappedTo[Long]

case class InventoryCountProductRow(
  batchId: InventoryCountBatchID,
  productId: ProductID,
  expected: Int,
  updatedAt: DateTime = DateTime.now(),
  counted: Option[Int] = None,
  synced: Boolean = false,
  isNew: Boolean = false,
  id: InventoryCountProductID = InventoryCountProductID(0)
)

case class InventoryCountProductDTO(
  id: InventoryCountProductID,
  sku: String,
  barcode: String,
  name: String,
  variation: Option[String],
  expected: Int,
  updatedAt: DateTime,
  counted: Option[Int],
  synced: Boolean,
  isNew: Boolean
)

case class InventoryCountDTO(
  id: InventoryCountBatchID,
  status: InventoryCountStatus,
  started: DateTime,
  finished: Option[DateTime],
  name: Option[String],
  category: Option[String],
  brand: Option[String]
)

object InventoryCountDTO {
  def fromRow(
    batch: InventoryCountBatchRow,
    category: Option[CategoryRow],
    brand: Option[BrandRow]
  ) =
    InventoryCountDTO(
      batch.id,
      batch.status,
      batch.started,
      batch.finished,
      batch.name,
      category.map(_.name),
      brand.map(_.name)
    )
}

object InventoryCountProductDTO {
  def fromRow(
    row: InventoryCountProductRow,
    product: ProductRow
  ) =
    InventoryCountProductDTO(
      row.id,
      product.sku,
      product.barcode,
      product.name,
      product.variation,
      row.expected,
      row.updatedAt,
      row.counted,
      row.synced,
      row.isNew
    )
}

case class PaginatedInventoryCountBatchesResponse(
  count: Int,
  batches: Seq[InventoryCountDTO]
)

case class PaginatedInventoryCountProductsResponse(
  counted: Int,
  uncounted: Int,
  products: Seq[InventoryCountProductDTO]
)
