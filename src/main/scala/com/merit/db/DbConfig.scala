package com.merit.db

import com.zaxxer.hikari.{HikariDataSource}
import slick.jdbc.JdbcBackend.Database
import slick.util.AsyncExecutor
import slick.jdbc.JdbcBackend.DatabaseDef
import com.merit.DbConfig

class Db(
  settings: DbConfig
) {
  lazy val dataSource: HikariDataSource = new HikariDataSource {
    setJdbcUrl(settings.url)
    setUsername(settings.username)
    setPassword(settings.password)
    setMaximumPoolSize(settings.maxPoolSize)
    setDriverClassName(settings.driver)
  }

  lazy val executor =
    AsyncExecutor("db", numThreads = settings.numThreads, queueSize = 1000)

  lazy val connect = Database.forDataSource(
    dataSource,
    maxConnections = Some(settings.maxPoolSize),
    executor = executor
  )
}

object Db {
  def apply(settings: DbConfig): DatabaseDef = new Db(settings).connect
}
