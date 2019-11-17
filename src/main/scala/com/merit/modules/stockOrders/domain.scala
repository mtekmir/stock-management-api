package com.merit.modules.stockOrders

import org.joda.time.DateTime
import slick.lifted.MappedTo

case class StockOrderID(value: Long) extends AnyVal with MappedTo[Long]

case class StockOrderRow(
  date: DateTime,
  id: StockOrderID = StockOrderID(0L)
)