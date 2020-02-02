package com.merit.api.stats

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.statsService.StatsService
import akka.http.scaladsl.server.Route
import org.joda.time.DateTime
import com.merit.modules.statsService.StatsDateFilter
import com.merit.api.CommonMatchers._

object TopSellingProductsRoute extends Directives with JsonSupport {
  def apply(statsService: StatsService): Route = {
    (path("stats" / "top-selling-products") & startDateMatcher & endDateMatcher) {
      (startDate, endDate) =>
        complete(statsService.getTopSellingProducts(StatsDateFilter(startDate, endDate)))
    }
  }
}
