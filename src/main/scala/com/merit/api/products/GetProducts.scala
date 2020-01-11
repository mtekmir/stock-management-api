package com.merit.api.products
import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.products.ProductService
import akka.http.scaladsl.server.Route

object GetProducts extends Directives with JsonSupport {
  def apply(productService: ProductService): Route =
    (get & pathEndOrSingleSlash & parameter('page ? 1, 'rowsPerPage ? 10)) {
      (page, rowsPerPage) =>
        complete(productService.getProducts(page, rowsPerPage))
    }
}
