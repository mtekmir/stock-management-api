package com.merit.api.products

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.products.ProductService
import akka.http.scaladsl.server.Route
import com.merit.modules.products.{EditProductRequest, ProductID}
import akka.http.scaladsl.server.PathMatcher1
import akka.http.scaladsl.model.StatusCodes
import scala.util.Success
import scala.util.Failure

object EditProduct extends Directives with JsonSupport {
  val productIdMatcher: PathMatcher1[ProductID] = LongNumber.map(ProductID(_))
  def apply(productService: ProductService): Route =
    (patch & pathPrefix(productIdMatcher) & entity(as[EditProductRequest])) { (id, req) =>
      onComplete(productService.editProduct(id, req)) {
        case Failure(exception) =>
          complete(StatusCodes.InternalServerError -> "Something went wrong")
        case Success(Left(errMessage)) => complete(StatusCodes.BadRequest -> errMessage)
        case Success(Right(product))   => complete(product)
      }
    }
}
