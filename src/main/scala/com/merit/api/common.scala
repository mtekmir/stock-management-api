package com.merit.api

import akka.http.scaladsl.server.Directives._
import org.joda.time.DateTime

object CommonMatchers {
  val startDateMatcher =
    parameter('startDate.?)
      .map(_.map(d => DateTime.parse(d)).getOrElse(DateTime.now().minusDays(30)))
  val endDateMatcher =
    parameter('endDate.?).map(_.map(d => DateTime.parse(d)).getOrElse(DateTime.now()))
}
