package com.merit.modules.brands

import db.Schema

trait BrandRepo[DbTask[_]] {
  def getByName(name: String): DbTask[Option[BrandRow]]
  def getAll: DbTask[Seq[BrandRow]]
  def insert(brand: BrandRow): DbTask[BrandRow]
  def batchInsert(brands: Seq[BrandRow]): DbTask[Seq[BrandID]]
}

object BrandRepo {
  def apply(schema: Schema) = new BrandRepo[slick.dbio.DBIO] {
    import schema._
    import schema.profile.api._

    def getByName(name: String): DBIO[Option[BrandRow]] =
      brands.filter(_.name === name).result.headOption

    def getAll: DBIO[Seq[BrandRow]] =
      brands.result

    def insert(brand: BrandRow): DBIO[BrandRow] =
      brands returning brands += brand

    def batchInsert(bs: Seq[BrandRow]): DBIO[Seq[BrandID]] =
      brands returning brands.map(_.id) ++= bs
  }
}
