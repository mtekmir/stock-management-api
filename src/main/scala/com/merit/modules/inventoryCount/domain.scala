package com.merit.modules.inventoryCount

import org.joda.time.DateTime
import slick.lifted.MappedTo
import com.merit.modules.products.{ProductID, ProductDTO, ProductRow}
import com.merit.modules.brands.{BrandRow, BrandID}
import com.merit.modules.categories.{CategoryRow, CategoryID}

case class InventoryCountBatchID(value: Long) extends AnyVal with MappedTo[Long]

object InventoryCountStatus extends Enumeration {
  type InventoryCountStatus = Value
  val Open      = Value("Open")
  val Completed = Value("Completed")
  val Cancelled = Value("Cancelled")
}

case class InventoryCountBatchRow(
  started: DateTime,
  finished: Option[DateTime],
  name: Option[String],
  categoryId: Option[CategoryID],
  brandId: Option[BrandID],
  status: InventoryCountStatus.Value = InventoryCountStatus.Open,
  id: InventoryCountBatchID = InventoryCountBatchID(0)
)

case class InventoryCountProductID(value: Long) extends AnyVal with MappedTo[Long]

case class InventoryCountProductRow(
  batchId: InventoryCountBatchID,
  productId: ProductID,
  expected: Int,
  counted: Option[Int] = None,
  synced: Boolean = false,
  id: InventoryCountProductID = InventoryCountProductID(0)
)

case class InventoryCountDTOProduct(
  id: ProductID,
  sku: String,
  barcode: String,
  name: String,
  variation: Option[String],
  expected: Int,
  counted: Option[Int],
  synced: Boolean
)

case class InventoryCountDTO(
  id: InventoryCountBatchID,
  status: InventoryCountStatus.Value,
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

object InventoryCountDTOProduct {
  def fromRow(
    row: InventoryCountProductRow,
    product: ProductRow
  ) =
    InventoryCountDTOProduct(
      product.id,
      product.sku,
      product.barcode,
      product.name,
      product.variation,
      row.expected,
      row.counted,
      row.synced
    )

}

case class PaginatedInventoryCountBatchesResponse(
  count: Int,
  batches: Seq[InventoryCountDTO]
)
