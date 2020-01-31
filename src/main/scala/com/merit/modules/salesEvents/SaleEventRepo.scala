package com.merit.modules.salesEvents

import db.Schema
import com.merit.modules.sales.SaleID
import com.merit.modules.users.{UserID, UserRow}
import com.merit.modules.excel.ExcelSaleRow
import com.merit.modules.sales.SaleSummary
import com.merit.modules.sales.SaleOutlet

trait SaleEventRepo[DbTask[_]] {
  def insertSaleCreatedEvent(saleId: SaleID, userId: UserID): DbTask[SaleEventRow]
  def insertSaleImportedEvent(
    saleId: SaleID,
    userId: UserID,
    found: Seq[ExcelSaleRow],
    notFound: Seq[ExcelSaleRow],
    outlet: SaleOutlet.Value
  ): DbTask[SaleEventRow]
  def insertSaleSyncedEvent(
    saleId: SaleID,
    totalProducts: Int,
    totalSynced: Int
  ): DbTask[SaleEventRow]
  def getAll(limit: Int): DbTask[Seq[(SaleEventRow, Option[UserRow])]]
}

object SaleEventRepo {
  def apply(schema: Schema) = new SaleEventRepo[slick.dbio.DBIO] {
    import schema._
    import schema.profile.api._

    def insertSaleCreatedEvent(saleId: SaleID, userId: UserID): DBIO[SaleEventRow] =
      salesEvents returning salesEvents += SaleEventRow(
        SaleEventType.SaleCreated,
        s"Sale created with id $saleId",
        saleId,
        Some(userId)
      )

    def insertSaleImportedEvent(
      saleId: SaleID,
      userId: UserID,
      found: Seq[ExcelSaleRow],
      notFound: Seq[ExcelSaleRow],
      outlet: SaleOutlet.Value
    ): DBIO[SaleEventRow] =
      salesEvents returning salesEvents += SaleEventRow(
        SaleEventType.SaleImported,
        s"""Sale imported with id of $saleId. 
           |Product Count: ${found.length}, Total Quantity: ${found.map(_.qty).sum}, Outlet: ${outlet.toString}
           |Not Found: 
           |${notFound.map(_.barcode).mkString("\n")}
           |""".stripMargin,
        saleId,
        Some(userId)
      )

    def insertSaleSyncedEvent(
      saleId: SaleID,
      totalProducts: Int,
      totalSynced: Int
    ): DBIO[SaleEventRow] =
      salesEvents returning salesEvents += SaleEventRow(
        SaleEventType.SaleSynced,
        s"Sale Synced with id of $saleId. $totalSynced products out of $totalProducts",
        saleId,
        None
      )

    def getAll(limit: Int): DBIO[Seq[(SaleEventRow, Option[UserRow])]] =
      salesEvents.take(limit).joinLeft(users).on(_.userId === _.id).result
  }
}
