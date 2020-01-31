package com.merit.modules.statsService

import com.merit.modules.products.Currency

case class TopSellingProduct(
  sku: String,
  name: String,
  variation: Option[String],
  soldQty: Int
)

case class PaginatedTopSellingProducts(
  count: Int,
  products: Seq[TopSellingProduct]
)

case class InventorySummary(
  currentStock: Int,
  stockValue: Currency
)
