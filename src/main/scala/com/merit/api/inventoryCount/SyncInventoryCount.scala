package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import com.merit.external.crawler.SyncStockOrderResponse
import scala.util.Failure
import scala.util.Success
import akka.http.scaladsl.model.StatusCodes.{OK, InternalServerError}
import com.merit.modules.inventoryCount.InventoryCountService
import com.merit.external.crawler.SyncInventoryCountResponse

object SyncInventoryCount extends Directives with JsonSupport {
  def apply(
  inventoryCountService: InventoryCountService,
    crawlerAuthenticator: (Credentials) => Option[Boolean]
  ): Route =
    (path("synced-inventory-count") & entity(as[SyncInventoryCountResponse])) { s =>
      authenticateBasic("crawler route", crawlerAuthenticator) { _ =>
        onComplete(inventoryCountService.saveSyncResult(s)) {
          case Failure(exception) => complete(InternalServerError -> exception)
          case Success(_)         => complete(OK)
        }
      }
    }
}
