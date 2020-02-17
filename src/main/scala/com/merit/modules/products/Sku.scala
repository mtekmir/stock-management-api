package com.merit.modules.products

import scala.util.Random

object Sku {
  def random: String =
    "SKU" + (1000000 + Random.nextInt(9999999)).toString
}