package com.merit.api.events

import api.JsonSupport
import akka.http.scaladsl.server.Directives
import com.merit.modules.salesEvents.SaleEventService
import akka.http.scaladsl.server.Route

object SaleEventsRoute extends Directives with JsonSupport {
  def apply(saleEventService: SaleEventService): Route =
    path("events" / "sales") {
      complete(saleEventService.getEvents(10))
    }
}
