package com.merit.modules

package object products {
  implicit class C1(
    val r: CreateProductRequest
  ) {
    def toRow(): ProductRow = {
      import r._
      ProductRow(
        barcode,
        sku,
        name,
        price,
        discountPrice,
        qty,
        variation,
        taxRate,
        brandId,
        categoryId
      )
    }
  }
}
