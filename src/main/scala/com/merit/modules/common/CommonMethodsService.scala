package com.merit.modules.common

import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import com.merit.modules.brands.BrandRow
import com.merit.modules.brands.BrandRepo
import com.merit.modules.categories.CategoryRepo
import scala.concurrent.ExecutionContext
import slick.jdbc.PostgresProfile.api._
import com.merit.modules.categories.CategoryRow
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID

trait CommonMethodsService {
  def insertBrandIfNotExists(brandName: String): DBIO[BrandRow]
  def insertCategoryIfNotExists(categoryName: String): DBIO[CategoryRow]
  def getBrandsMap(brandNames: Seq[String]): DBIO[Map[String, BrandID]]
  def getCategoryMap(categoryNames: Seq[String]): DBIO[Map[String, CategoryID]]
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

    def getBrandsMap(brandNames: Seq[String]): slick.dbio.DBIO[Map[String, BrandID]] =
      DBIO
        .sequence(brandNames.map(insertBrandIfNotExists(_)))
        .map(
          _.foldLeft(Map[String, BrandID]())((m, b) => m + (b.name -> b.id))
        )

    def getCategoryMap(categoryNames: Seq[String]): slick.dbio.DBIO[Map[String, CategoryID]] =
      DBIO
        .sequence(categoryNames.map(insertCategoryIfNotExists(_)))
        .map(
          _.foldLeft(Map[String, CategoryID]())((m, b) => m + (b.name -> b.id))
        )
  }
}
