package com.merit.modules.brands

import db.Schema

trait BrandRepo[DbTask[_]] {
  def getByName(name: String): DbTask[Option[BrandRow]]
  def getAll(): DbTask[Seq[BrandRow]]
  def add(brand: BrandRow): DbTask[BrandID]
  def batchInsert(brands: Seq[BrandRow]): DbTask[Seq[BrandID]]
}

object BrandRepo {
  def apply(schema: Schema) = new BrandRepo[slick.dbio.DBIO] {
    import schema._
    import schema.profile.api._

    def getByName(name: String): DBIO[Option[BrandRow]] =
      brands.filter(_.name === name).result.headOption

    def getAll(): DBIO[Seq[BrandRow]] =
      brands.result

    def add(brand: BrandRow): DBIO[BrandID] =
      brands returning brands.map(_.id) += brand

    def batchInsert(bs: Seq[BrandRow]): DBIO[Seq[BrandID]] =
      brands returning brands.map(_.id) ++= bs
  }
}
