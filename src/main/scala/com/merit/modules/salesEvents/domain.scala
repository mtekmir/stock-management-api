package com.merit.modules.salesEvents

import slick.lifted.MappedTo
import com.merit.modules.users.UserID
import com.merit.modules.sales.SaleID
import org.joda.time.DateTime

object SaleEventType extends Enumeration {
  type SaleEventType = Value

  val SaleCreated  = Value("Sale created")
  val SaleImported = Value("Sale imported")
  val SaleSynced   = Value("Sale synced")
}

case class SaleEventID(value: Long) extends AnyVal with MappedTo[Long]

case class SaleEventRow(
  event: SaleEventType.Value,
  message: String,
  saleId: SaleID,
  userId: Option[UserID],
  created: DateTime = DateTime.now(),
  id: SaleEventID = SaleEventID(0)
)

case class SaleEventDTO(
  id: SaleEventID,
  event: SaleEventType.Value,
  message: String,
  saleId: SaleID,
  userId: Option[String],
  created: DateTime
)
