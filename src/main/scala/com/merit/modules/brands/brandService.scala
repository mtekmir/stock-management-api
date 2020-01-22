package com.merit.modules.brands

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend
import scala.concurrent.Future
import com.merit.modules.excel.ExcelProductRow
import slick.jdbc.PostgresProfile.api._

trait BrandService {
  def batchInsert(bs: Seq[BrandRow]): Future[Seq[BrandRow]]
  def create(brand: BrandRow): Future[BrandRow]
  def getBrands: Future[Seq[BrandRow]]
}

object BrandService {
  def apply(db: Database, brandRepo: BrandRepo[DBIO])(
    implicit ec: ExecutionContext
  ) = new BrandService {
    private def insertIfNotExists(brand: BrandRow): DBIO[BrandRow] =
      brandRepo.get(brand.name).flatMap {
        case Some(b) => DBIO.successful(b)
        case None    => brandRepo.insert(brand)
      }.transactionally

    def batchInsert(bs: Seq[BrandRow]): Future[Seq[BrandRow]] = {
      val action = bs.map(insertIfNotExists)

      db.run(DBIO.sequence(action))
    }

    def create(brand: BrandRow): Future[BrandRow] =
      db.run(insertIfNotExists(brand))

    def getBrands: Future[Seq[BrandRow]] = 
      db.run(brandRepo.getAll)
  }
}
