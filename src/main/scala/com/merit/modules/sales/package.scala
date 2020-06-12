package com.merit.modules

import com.merit.modules.products.ProductDTO

package object sales {

  import scala.collection.immutable.ListMap

  import com.merit.modules.excel.ExcelWebSaleRow
  implicit class SaleOps(
    val sale: SaleRow
  ) {
    def toDTO(products: Seq[SaleDTOProduct]): SaleDTO = {
      import sale._
      SaleDTO(
        id,
        createdAt,
        outlet,
        status,
        orderNo,
        total,
        discount,
        description,
        paymentMethod,
        products
      )
    }

    def toSummary(products: Seq[ProductDTO]) = {
      import sale._
      SaleSummary(
        sale.id,
        sale.createdAt,
        sale.total,
        sale.discount,
        sale.outlet,
        sale.status,
        sale.description,
        sale.paymentMethod,
        products.map(p => SaleSummaryProduct.fromProductDTO(p, p.qty))
      )
    }
  }

  implicit class SaleOps2(
    val rows: Seq[ExcelWebSaleRow]
  ) {
    def distinctByOrderNoWithoutProducts: Seq[WebSaleRow] =
      rows
        .foldLeft(ListMap[String, WebSaleRow]()) {
          case (m, row) =>
            m.get(row.orderNo) match {
              case None => {
                import row._
                m + (orderNo -> WebSaleRow(
                  orderNo,
                  total,
                  discount,
                  createdAt,
                  status,
                  paymentMethod,
                  Seq()
                ))
              }
              case Some(_) => m
            }
        }
        .values
        .toList
  }
}
