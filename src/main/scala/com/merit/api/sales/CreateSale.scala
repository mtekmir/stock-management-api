package com.merit.api.sales

import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import api.{JsonSupport, CreateSaleRequest}
import com.merit.modules.sales.SaleService

object CreateSale extends Directives with JsonSupport {
  def apply(saleService: SaleService): Route =
    (post & pathEndOrSingleSlash & entity(as[CreateSaleRequest])) {
      case CreateSaleRequest(total, discount, products) if products.length > 0 =>
        complete(saleService.createSale(total, discount, products))
      case _ => complete(BadRequest -> "Products must not be empty")
    }
}
