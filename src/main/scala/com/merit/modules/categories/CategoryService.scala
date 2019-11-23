package com.merit.modules.categories

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Future
import com.merit.modules.excel.ExcelProductRow

trait CategoryService {
  def getAll: Future[Seq[CategoryRow]]
  def batchInsert(names: Seq[String]): Future[Seq[CategoryRow]]
  def insert(name: String): Future[CategoryRow]
}

object CategoryService {
  def apply(db: Database, categoryRepo: CategoryRepo[DBIO])(
    implicit ec: ExecutionContext
  ) =
    new CategoryService {
      private def insertIfNotExists(category: CategoryRow): DBIO[CategoryRow] =
        categoryRepo
          .getByName(category.name)
          .flatMap {
            case None    => categoryRepo.insert(category)
            case Some(c) => DBIO.successful(c)
          }
          .transactionally

      def batchInsert(names: Seq[String]): Future[Seq[CategoryRow]] =
        db.run(DBIO.sequence(names.map(n => insertIfNotExists(CategoryRow(n)))))

      def insert(name: String): Future[CategoryRow] =
        db.run(insertIfNotExists(CategoryRow(name)))
      
      def getAll: Future[Seq[CategoryRow]] = 
        db.run(categoryRepo.getAll)
    }
}
