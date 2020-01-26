package com.merit.api.products

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.products.ProductService
import akka.http.scaladsl.server.Route
import com.merit.modules.products.CreateProductRequest
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes
import scala.util.Success

object CreateProduct extends Directives with JsonSupport {
  def apply(productService: ProductService): Route =
    (post & pathEndOrSingleSlash & entity(as[CreateProductRequest])) { req =>
      onComplete(productService.createProduct(req)) {
        case Failure(_)                => complete(StatusCodes.InternalServerError -> "Something went wrong")
        case Success(Right(product))   => complete(product)
        case Success(Left(errMessage)) => complete(StatusCodes.BadRequest -> errMessage)
      }
    }
}
