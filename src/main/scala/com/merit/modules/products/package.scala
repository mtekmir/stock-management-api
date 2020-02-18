package com.merit.modules

package object products {

  import scala.util.Random
  implicit class C1(
    val r: CreateProductRequest
  ) {
    def toRow(): ProductRow = {
      import r._
      ProductRow(
        barcode,
        sku.getOrElse(Sku.random),
        name.getOrElse("Empty" + Random.nextInt(100000)),
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
