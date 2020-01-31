package com.merit.db

import org.joda.time.DateTime
import slick.lifted._
import java.sql.Timestamp
import com.merit.modules.products.Currency
import com.merit.modules.inventoryCount.InventoryCountStatus
import com.merit.modules.sales.SaleOutlet
import com.merit.modules.salesEvents.SaleEventType
import db.Schema

trait DbMappers { this: Schema =>
  import profile.api._

  implicit val jodaDateTimeType =
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts.getTime)
    )

  implicit val currencyType = MappedColumnType.base[Currency, BigDecimal](
    c => c.value,
    bd => Currency.fromDb(bd)
  )

  implicit val myEnumMapper = MappedColumnType.base[InventoryCountStatus.Value, String](
    e => e.toString,
    s => InventoryCountStatus.withName(s)
  )

  implicit val saleOutletMapper = MappedColumnType.base[SaleOutlet.Value, String](
    e => e.toString,
    s => SaleOutlet.withName(s)
  )

  implicit val saleEventMapper = MappedColumnType.base[SaleEventType.Value, String](
    e => e.toString,
    s => SaleEventType.withName(s)
  )

}
