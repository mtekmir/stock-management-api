package com.merit.modules.brands

import slick.dbio.DBIO
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend
import scala.concurrent.Future
import slick.jdbc.PostgresProfile
import com.merit.modules.excel.ExcelProductRow

trait BrandService {
  // def insertIfNotExists(brand: BrandRow): Task[BrandID]
  def batchInsert(bs: Vector[BrandRow]): Future[Vector[BrandID]]
  def batchInsertExcelRows(rows: Vector[ExcelProductRow]): Future[Vector[BrandID]]
  def insert(brand: BrandRow): Future[BrandID]
}

object BrandService {
  def apply(db: PostgresProfile.backend.Database, brandRepo: BrandRepo[DBIO])(
    implicit ec: ExecutionContext
  ) = new BrandService {
    private def insertIfNotExists(brand: BrandRow): DBIO[BrandID] =
      brandRepo.getByName(brand.name).flatMap {
        case Some(b) => DBIO.successful(b.id)
        case None    => brandRepo.add(brand)
      }

    def batchInsert(bs: Vector[BrandRow]): Future[Vector[BrandID]] = {
      val action = bs.map(insertIfNotExists)

      db.run(DBIO.sequence(action))
    }

    def batchInsertExcelRows(rows: Vector[ExcelProductRow]): Future[Vector[BrandID]] =
      batchInsert(rows.map {
        case ExcelProductRow(_, _, _, _, _, _, brand) => BrandRow(brand)
      })

    def insert(brand: BrandRow): Future[BrandID] =
      db.run(insertIfNotExists(brand))
  }
}
