package com.merit.api.stockOrders
import akka.http.scaladsl.server.Directives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import com.merit.modules.stockOrders.StockOrderService
import akka.http.scaladsl.server.directives.Credentials
import com.merit.external.crawler.SyncStockOrderResponse
import scala.util.Failure
import scala.util.Success
import akka.http.scaladsl.model.StatusCodes.{OK, InternalServerError}

object SyncStockOrderRoute extends Directives with JsonSupport {
  def apply(
    stockOrderService: StockOrderService,
    crawlerAuthenticator: (Credentials) => Option[Boolean]
  ): Route =
    (path("synced-stock-order") & entity(as[SyncStockOrderResponse])) { s =>
      authenticateBasic("crawler route", crawlerAuthenticator) { _ =>
        onComplete(stockOrderService.saveSyncResult(s)) {
          case Failure(exception) => complete(InternalServerError -> exception)
          case Success(_)         => complete(OK)
        }
      }
    }
}
