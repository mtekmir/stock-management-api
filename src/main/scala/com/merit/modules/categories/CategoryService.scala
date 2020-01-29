package com.merit.modules.categories

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Future
import com.merit.modules.excel.ExcelProductRow
import com.typesafe.scalalogging.LazyLogging

trait CategoryService {
  def getCategories: Future[Seq[CategoryRow]]
  def create(name: String): Future[CategoryRow]
}

object CategoryService {
  def apply(db: Database, categoryRepo: CategoryRepo[DBIO])(
    implicit ec: ExecutionContext
  ) = new CategoryService with LazyLogging {
    def create(name: String): Future[CategoryRow] = {
      logger.info(s"Creating a new category with name: $name")
      db.run(
        categoryRepo
          .getByName(name)
          .flatMap {
            case None    => categoryRepo.insert(CategoryRow(name))
            case Some(c) => DBIO.successful(c)
          }
          .transactionally
      )
    }

    def getCategories: Future[Seq[CategoryRow]] =
      db.run(categoryRepo.getAll)
  }
}
