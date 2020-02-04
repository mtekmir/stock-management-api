package com.merit.api.stockOrders

import api.JsonSupport
import com.merit.modules.stockOrders.StockOrderService
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives

object GetStockOrdersRoute extends Directives with JsonSupport {
  def apply(stockOrderService: StockOrderService): Route = 
    path("stock-orders") {
      complete(stockOrderService.getStockOrders())
    }
}