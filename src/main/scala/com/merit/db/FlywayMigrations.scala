package com.merit.db

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

object RunFlywayMigrations {
  def apply(dataSource: HikariDataSource) =
    Flyway.configure.dataSource(dataSource).load.migrate
}
