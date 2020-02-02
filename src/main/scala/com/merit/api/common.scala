package com.merit.api

import akka.http.scaladsl.server.Directives._
import org.joda.time.DateTime

object CommonMatchers {
  val now       = DateTime.now()
  val startDate = now.minusDays(30)

  val startDateMatcher =
    parameter('startDate.?).map(_.map(d => DateTime.parse(d)).getOrElse(startDate))
  val endDateMatcher =
    parameter('endDate.?).map(_.map(d => DateTime.parse(d)).getOrElse(now))
}
