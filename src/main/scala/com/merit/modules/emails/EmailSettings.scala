package com.merit.modules.emails

case class EmailSettings(
  host: String,
  port: Int,
  username: String,
  password: String
)