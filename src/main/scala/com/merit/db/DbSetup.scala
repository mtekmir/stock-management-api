package com.merit.db

import com.zaxxer.hikari.{HikariDataSource}
import slick.jdbc.JdbcBackend.Database
import slick.util.AsyncExecutor
import slick.jdbc.JdbcBackend.DatabaseDef
import com.merit.DbConfig

trait DbSetup {
  def connect(): DatabaseDef
  def migrate(): Int
}

object DbSetup {
  def apply(settings: DbConfig): DbSetup = new DbSetup {
    private lazy val dataSource: HikariDataSource = new HikariDataSource {
      setJdbcUrl(settings.url)
      setUsername(settings.username)
      setPassword(settings.password)
      setMaximumPoolSize(settings.maxPoolSize)
      setDriverClassName(settings.driver)
    }

    lazy val executor =
      AsyncExecutor("db", numThreads = settings.numThreads, queueSize = 1000)

    def migrate() = RunFlywayMigrations(dataSource)

    def connect() = Database.forDataSource(
      dataSource,
      maxConnections = Some(settings.maxPoolSize),
      executor = executor
    )
  }
}
