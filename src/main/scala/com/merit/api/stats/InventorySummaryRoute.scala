package com.merit.api.stats

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.statsService.StatsService
import akka.http.scaladsl.server.Route

object InventorySummaryRoute extends Directives with JsonSupport {
  def apply(statsService: StatsService): Route = {
    (path("stats" / "inventory-summary")) {
      complete(statsService.getInventorySummary())
    }
  }
}