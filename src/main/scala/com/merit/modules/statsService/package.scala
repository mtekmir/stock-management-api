package com.merit.modules

import org.joda.time.DateTime
import java.util.Calendar

package object statsService {
  implicit class DTimeOps(d: DateTime) {
    val cal = Calendar.getInstance()
    def getWeek: Int = {
      cal.setFirstDayOfWeek(Calendar.MONDAY)
      cal.setTimeInMillis(d.getMillis)
      cal.get(Calendar.WEEK_OF_YEAR)
    }
    def getMonth: Int = {
      cal.setFirstDayOfWeek(Calendar.MONDAY)
      cal.setTimeInMillis(d.getMillis)
      cal.get(Calendar.MONTH)
    }
    def isSameMonth(d2: DateTime): Boolean =
      d.getMonth == d2.getMonth && d.getYear == d2.getYear

    def isSameWeek(d2: DateTime): Boolean =
      d.getWeek == d2.getWeek && d.getYear == d2.getYear

    def isSameDay(d2: DateTime): Boolean =
      d.getDayOfYear == d2.getDayOfYear && d.getYear == d2.getYear
  }
}
