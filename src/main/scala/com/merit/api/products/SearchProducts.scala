package com.merit.api.products

import akka.http.scaladsl.server.{Directives, Route}
import api.JsonSupport
import com.merit.modules.products.{ProductService, ProductDTO}

object SearchProducts extends Directives with JsonSupport {
  def apply(productService: ProductService): Route =
    (pathPrefix("search") & parameter('q.as[String])) { query =>
      query match {
        case "" => complete(Seq[ProductDTO]())
        case _  => complete(productService.searchProducts(query))
      }
    }
}
