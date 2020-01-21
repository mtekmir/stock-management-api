package com.merit.api.products

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.products.{ProductService, ProductFilters}
import akka.http.scaladsl.server.Route
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID

object GetProducts extends Directives with JsonSupport {
  def apply(productService: ProductService): Route =
    (get & pathEndOrSingleSlash & parameter(
      'page ? 1,
      'rowsPerPage ? 10,
      'brandId.as[Int].?,
      'categoryId.as[Int].?
    )) { (page, rowsPerPage, brandId, categoryId) =>
      complete(
        productService.getProducts(
          page,
          rowsPerPage,
          ProductFilters(categoryId.map(CategoryID(_)), brandId.map(BrandID(_)))
        )
      )
    }
}
