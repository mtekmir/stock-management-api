package com.merit.api.stats

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.statsService.StatsService
import akka.http.scaladsl.server.Route
import com.merit.api.CommonMatchers._
import com.merit.modules.statsService.StatsDateFilter

object SaleStatsRoute extends Directives with JsonSupport {
  def apply(statsService: StatsService): Route =
    (path("stats" / "sale-stats") & startDateMatcher & endDateMatcher) {
      (startDate, endDate) =>
        complete(statsService.getStats(StatsDateFilter(startDate, endDate)))
    }
}
