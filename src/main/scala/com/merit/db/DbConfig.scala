package com.merit.db

import com.zaxxer.hikari.{HikariDataSource, HikariConfig}
import slick.jdbc.JdbcBackend.Database
import slick.util.AsyncExecutor
import slick.jdbc.JdbcBackend.DatabaseDef

case class DbConfig(
  username: String,
  password: String,
  url: String,
  driver: String,
  maxPoolSize: Int,
  numThreads: Int,
  cachePrepStmts: Boolean,
  prepStmtCacheSize: Int,
  prepStmtCacheSqlLimit: Int,
  useServerPrepStmts: Boolean
)

class Db(
  settings: DbConfig
) {
  lazy val dataSource: HikariDataSource = {
    val jdbcConfig = new HikariConfig()
    jdbcConfig.setJdbcUrl(settings.url)
    jdbcConfig.setUsername(settings.username)
    jdbcConfig.setPassword(settings.password)
    jdbcConfig.setMaximumPoolSize(settings.maxPoolSize)
    jdbcConfig.setDriverClassName(settings.driver)

    jdbcConfig.addDataSourceProperty("cachePrepStmts", settings.cachePrepStmts)
    jdbcConfig.addDataSourceProperty("prepStmtCacheSize", settings.prepStmtCacheSize)
    jdbcConfig.addDataSourceProperty(
      "prepStmtCacheSqlLimit",
      settings.prepStmtCacheSqlLimit
    )
    jdbcConfig.addDataSourceProperty("useServerPrepStmts", settings.useServerPrepStmts)

    new HikariDataSource(jdbcConfig)
  }

  lazy val executor =
    AsyncExecutor("test1", numThreads = settings.numThreads, queueSize = 1000)

  lazy val connect = Database.forDataSource(
    dataSource,
    maxConnections = Some(settings.maxPoolSize),
    executor = executor
  )
}

object Db {
  def apply(settings: DbConfig): DatabaseDef = new Db(settings).connect
}
