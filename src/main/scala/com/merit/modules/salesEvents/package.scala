package com.merit.modules

import com.merit.modules.users.UserRow

package object salesEvents {
  implicit class SaleEventOps(
    val row: SaleEventRow
  ) {
    def toDTO(userRow: Option[UserRow]): SaleEventDTO = {
      import row._
      SaleEventDTO(
        id,
        event,
        message,
        saleId,
        userRow.map(_.name),
        created
      )
    }

  }
}
