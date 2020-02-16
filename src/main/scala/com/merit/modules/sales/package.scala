package com.merit.modules

package object sales {
  implicit class SaleOps(
    val sale: SaleRow
  ) {
    def toDTO(products: Seq[SaleDTOProduct]): SaleDTO = {
      import sale._
      SaleDTO(id, createdAt, outlet, status, orderNo, total, discount, products)
    }
  }
}