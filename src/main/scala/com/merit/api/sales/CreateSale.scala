package com.merit.api.sales

import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import api.{JsonSupport, CreateSaleRequest}
import com.merit.modules.sales.SaleService
import com.merit.modules.users.UserID

object CreateSale extends Directives with JsonSupport {
  def apply(saleService: SaleService, userId: UserID): Route =
    (post & pathEndOrSingleSlash & entity(as[CreateSaleRequest])) {
      case CreateSaleRequest(total, discount, products) if products.length > 0 =>
        complete(saleService.create(total, discount, products, userId))
      case _ => complete(BadRequest -> "Products must not be empty")
    }
}
