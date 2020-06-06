package com.merit.modules.common

import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import com.merit.modules.brands.BrandRow
import com.merit.modules.brands.BrandRepo
import com.merit.modules.categories.CategoryRepo
import scala.concurrent.ExecutionContext
import slick.jdbc.PostgresProfile.api._
import com.merit.modules.categories.CategoryRow

trait CommonMethodsService {
  def insertBrandIfNotExists(brandName: String): DBIO[BrandRow]
  def insertCategoryIfNotExists(categoryName: String): DBIO[CategoryRow]
}

object CommonMethodsService {
  def apply(
    db: Database,
    brandRepo: BrandRepo[DBIO],
    categoryRepo: CategoryRepo[DBIO]
  )(implicit ec: ExecutionContext) = new CommonMethodsService {
    def insertBrandIfNotExists(brandName: String): DBIO[BrandRow] =
      brandRepo
        .get(brandName)
        .flatMap {
          case Some(b) => DBIO.successful(b)
          case None    => brandRepo.insert(BrandRow(brandName))
        }
        .transactionally

    def insertCategoryIfNotExists(categoryName: String): DBIO[CategoryRow] =
      categoryRepo
        .getByName(categoryName)
        .flatMap {
          case Some(c) => DBIO.successful(c)
          case None    => categoryRepo.insert(CategoryRow(categoryName))
        }
        .transactionally
  }
}
