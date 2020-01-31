package com.merit.api.stats

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.statsService.StatsService
import akka.http.scaladsl.server.Route

object TopSellingProductsRoute extends Directives with JsonSupport {
  def apply(statsService: StatsService): Route = {
    (path("stats" / "top-selling-products")) {
      complete(statsService.getTopSellingProducts())
    }
  }
}