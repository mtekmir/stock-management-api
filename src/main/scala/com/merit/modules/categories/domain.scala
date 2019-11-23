package com.merit.modules.categories
import slick.lifted.MappedTo

final case class CategoryID(value: Long) extends AnyVal with MappedTo[Long]

final case class CategoryRow(
  name: String,
  id: CategoryID = CategoryID(0L)
)