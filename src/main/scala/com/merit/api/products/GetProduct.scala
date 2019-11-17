package com.merit.api.products
import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.products.ProductService
import akka.http.scaladsl.server.{Route, PathMatcher1}
import com.merit.modules.products.ProductID
import scala.util.Success
import akka.http.scaladsl.model.StatusCodes
import scala.util.Failure

object GetProduct extends Directives with JsonSupport {
  val barcodeMatcher: PathMatcher1[String] = Segment
  def apply(productService: ProductService): Route = {
    (path(barcodeMatcher)) { barcode =>
      onComplete(productService.get(barcode)) {
        case Success(Some(product)) => complete(product)
        case Success(None) => complete(StatusCodes.NotFound)
        case Failure(e) => 
        println(e)
        complete(StatusCodes.InternalServerError)
      }
    }
  }
}