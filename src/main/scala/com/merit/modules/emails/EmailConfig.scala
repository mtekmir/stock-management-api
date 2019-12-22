package com.merit.modules.emails

case class EmailConfig(
  host: String,
  port: Int,
  username: String,
  password: String
)