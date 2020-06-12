package com.merit.api.sales

import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import api.{JsonSupport, CreateSaleRequest}
import com.merit.modules.sales.SaleService
import com.merit.modules.users.UserID
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.model.StatusCodes
import scala.util.Success
import scala.util.Failure
import com.merit.modules.sales.PaymentMethod

object CreateSale extends Directives with JsonSupport {
  def apply(saleService: SaleService, userId: UserID)(implicit ec: ExecutionContext): Route =
    (post & pathEndOrSingleSlash & entity(as[CreateSaleRequest])) {
      case CreateSaleRequest(_, _, _, paymentMethod, products)
          if products == Nil && paymentMethod == Some(PaymentMethod.OnCredit) =>
        complete(
          StatusCodes.BadRequest -> "Sale with no products cannot be completed with on credit payment"
        )
      case CreateSaleRequest(total, discount, description, paymentMethod, products) =>
        onComplete(
          saleService.create(
            total,
            discount,
            description,
            paymentMethod.getOrElse(PaymentMethod.Cash),
            products,
            userId
          )
        ) {
          case Success(Some(res)) => complete(StatusCodes.OK -> res)
          case Success(None)      => complete(StatusCodes.OK)
          case Failure(e)         => complete(StatusCodes.InternalServerError -> e)
        }
    }
}
