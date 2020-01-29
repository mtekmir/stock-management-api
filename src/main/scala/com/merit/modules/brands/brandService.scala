package com.merit.modules.brands

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend
import scala.concurrent.Future
import com.merit.modules.excel.ExcelProductRow
import slick.jdbc.PostgresProfile.api._
import com.typesafe.scalalogging.LazyLogging

trait BrandService {
  def create(brand: BrandRow): Future[BrandRow]
  def getBrands: Future[Seq[BrandRow]]
}

object BrandService {
  def apply(db: Database, brandRepo: BrandRepo[DBIO])(
    implicit ec: ExecutionContext
  ) = new BrandService with LazyLogging {
    def create(brand: BrandRow): Future[BrandRow] = {
      logger.info(s"Creating a new brand with name ${brand.name}")
      db.run(
        brandRepo
          .get(brand.name)
          .flatMap {
            case Some(b) => DBIO.successful(b)
            case None    => brandRepo.insert(brand)
          }
          .transactionally
      )
    }

    def getBrands: Future[Seq[BrandRow]] =
      db.run(brandRepo.getAll)
  }
}
