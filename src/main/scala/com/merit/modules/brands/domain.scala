package com.merit.modules.brands

import java.util.UUID
import slick.lifted.MappedTo

case class BrandID(value: Long) extends AnyVal with MappedTo[Long]

case class BrandRow(name: String, id: BrandID = BrandID(0L))
