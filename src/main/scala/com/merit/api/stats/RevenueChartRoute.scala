package com.merit.api.stats

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.statsService.StatsService
import akka.http.scaladsl.server.Route
import com.merit.api.CommonMatchers._
import com.merit.modules.statsService.StatsDateFilter
import com.merit.modules.statsService.ChartOption

object RevenueChartRoute extends Directives with JsonSupport {
  val optionMatcher = parameter('option.?).map {
    case None    => ChartOption.Daily
    case Some(o) => ChartOption.withName(o)
  }
  def apply(statsService: StatsService): Route =
    (path("stats" / "revenue-chart") & startDateMatcher & endDateMatcher & optionMatcher) {
      (startDate, endDate, option) =>
        complete(statsService.revenueChartData(StatsDateFilter(startDate, endDate), option))
    }
}
