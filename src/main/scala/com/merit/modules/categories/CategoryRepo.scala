package com.merit.modules.categories

import db.Schema
import com.merit.modules.brands.BrandRow

trait CategoryRepo[DbTask[_]] {
  def insert(category: CategoryRow): DbTask[CategoryRow]
  def getByName(name: String): DbTask[Option[CategoryRow]]
  def get(categoryId: Option[CategoryID]): DbTask[Option[CategoryRow]]
  def getAll: DbTask[Seq[CategoryRow]]
  def batchInsert(categories: Seq[CategoryRow]): DbTask[Seq[CategoryRow]]
}

object CategoryRepo {
  def apply(schema: Schema): CategoryRepo[slick.dbio.DBIO] =
    new CategoryRepo[slick.dbio.DBIO] {
      import schema._
      import schema.profile.api._

      def insert(category: CategoryRow): DBIO[CategoryRow] =
        categories returning categories += category

      def getByName(name: String): DBIO[Option[CategoryRow]] =
        categories.filter(_.name === name).result.headOption

      def get(categoryId: Option[CategoryID]): DBIO[Option[CategoryRow]] = 
        categoryId match {
          case None => DBIO.successful(None)
          case Some(id) => categories.filter(_.id === id).result.headOption
        }

      def getAll: DBIO[Seq[CategoryRow]] =
        categories.result

      def batchInsert(cs: Seq[CategoryRow]): slick.dbio.DBIO[Seq[CategoryRow]] =
        categories returning categories ++= cs
    }
}
