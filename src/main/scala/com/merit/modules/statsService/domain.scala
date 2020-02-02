package com.merit.modules.statsService

import com.merit.modules.products.Currency
import org.joda.time.DateTime

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

case class Stats(
  revenue: Currency,
  saleCount: Int,
  soldProductCount: Int
)

case class StatsDateFilter(
  startDate: DateTime,
  endDate: DateTime
)

case class Point(
  x: Int,
  y: Int
)