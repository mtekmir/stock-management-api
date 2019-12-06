package com.merit.modules.emails

case class EmailMessage(
  recipient: String,
  subject: String,
  content: String,
  attachment: Option[Any] = None
)