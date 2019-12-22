package com.merit.api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import com.merit.external.crawler.SyncSaleResponse
import com.merit.modules.sales.SaleService
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes.{OK, InternalServerError}
import akka.http.scaladsl.server.directives.Credentials
import com.merit.external.crawler.CrawlerClientConfig
import pureconfig._
import pureconfig.generic.auto._
import com.typesafe.config.ConfigFactory
import com.merit.modules.sales.SaleID

object SyncSaleRoute extends Directives with JsonSupport {
  private val crawlerClientConfig =
    loadConfigOrThrow[CrawlerClientConfig](ConfigFactory.load(), "crawler")

  def authenticator(credentials: Credentials): Option[Boolean] = {
    println(credentials)
    credentials match {
      case c @ Credentials.Provided(crawlerClientConfig.username)
          if c.verify(crawlerClientConfig.password) =>
        Some(true)
      case _ => None
    }
  }

  def apply(saleService: SaleService): Route =
    (path("synced-sale") & post) {
      entity(as[SyncSaleResponse]) { s =>
        authenticateBasic("crawler route", authenticator) { _ =>
          onComplete(saleService.saveSyncResult(s)) {
            case Failure(exception)    => complete(InternalServerError -> exception)
            case scala.util.Success(_) => complete(OK)
          }
        }
      }
    }
}
