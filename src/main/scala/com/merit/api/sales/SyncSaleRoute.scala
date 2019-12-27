package com.merit.api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import com.merit.external.crawler.SyncSaleResponse
import com.merit.modules.sales.SaleService
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes.{OK, InternalServerError}
import akka.http.scaladsl.server.directives.Credentials
import com.merit.CrawlerClientConfig
import pureconfig._
import pureconfig.generic.auto._
import com.typesafe.config.ConfigFactory
import com.merit.modules.sales.SaleID

object SyncSaleRoute extends Directives with JsonSupport {
  def apply(
    saleService: SaleService,
    crawlerAuthenticator: (Credentials) => Option[Boolean]
  ): Route =
    (path("synced-sale") & post) {
      entity(as[SyncSaleResponse]) { s =>
        authenticateBasic("crawler route", crawlerAuthenticator) { _ =>
          onComplete(saleService.saveSyncResult(s)) {
            case Failure(exception)    => complete(InternalServerError -> exception)
            case scala.util.Success(_) => complete(OK)
          }
        }
      }
    }
}
