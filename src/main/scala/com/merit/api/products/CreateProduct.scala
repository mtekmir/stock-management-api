package com.merit.api.products
import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.products.ProductService
import akka.http.scaladsl.server.Route
import com.merit.modules.products.CreateProductRequest

object CreateProduct extends Directives with JsonSupport {
  def apply(productService: ProductService): Route = {
    (post & pathEndOrSingleSlash & entity(as[CreateProductRequest])) { req =>
      complete(productService.create(req))
    }
  }
}