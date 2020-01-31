package com.merit.api.stats

import akka.http.scaladsl.server.Directives
import com.merit.modules.statsService.StatsService
import akka.http.scaladsl.server.Route

object StatsRoutes extends Directives {
  def apply(statsService: StatsService): Route =
    TopSellingProductsRoute(statsService)
}
