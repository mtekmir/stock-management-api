package com.merit.modules.products

import scala.util.Random

object Barcode {
  val countries = List("TR", "NL", "FR", "US")
  val countryCodes = Map(
    "TR" -> List("86"),
    "NL" -> List("87"),
    "FR" -> List("37", "36", "35", "34"),
    "US" -> List("01", "02", "03", "04")
  )

  private def randomFrom(col: Seq[String]) = col.drop(Random.nextInt(col.size)).head

  private def generateCountryCode: String =
    randomFrom(countryCodes.get(randomFrom(countries)).getOrElse(List("87")))

  private def generateManufacturerCode: String =
    (10000 + Random.nextInt(89999)).toString

  private def generateProductCode: String =
    (10000 + Random.nextInt(89999)).toString

  private def calculateCheckDigit(n: String): String = {
    val digits = n.split("").map(d => d.toInt)

    val weightedDigitsSum =
      digits(0) +
        digits(1) * 3 +
        digits(2) +
        digits(3) * 3 +
        digits(4) +
        digits(5) * 3 +
        digits(6) +
        digits(7) * 3 +
        digits(8) +
        digits(9) * 3 +
        digits(10) +
        digits(11) * 3

    val remainder = weightedDigitsSum % 10

    if (remainder == 0) "0" else (10 - remainder).toString
  }

  def random: String = {
    val s = generateCountryCode + generateManufacturerCode + generateProductCode
    s + calculateCheckDigit(s)
  }
}
