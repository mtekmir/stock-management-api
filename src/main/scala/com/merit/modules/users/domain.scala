package com.merit.modules.users

import slick.lifted.MappedTo
import java.util.UUID

case class UserID(value: UUID) extends AnyVal with MappedTo[UUID]

object UserID {
  def from(str: String): UserID = UserID(UUID.fromString(str))
  def random = UserID(UUID.randomUUID())
}

case class UserRow(
  email: String,
  name: String,
  password: String,
  id: UserID = UserID(UUID.randomUUID())
)

case class UserDTO(
  id: UserID,
  email: String,
  name: String
)

object UserDTO {
  def fromRow(row: UserRow): UserDTO = {
    import row._
    UserDTO(id, email, name)
  }
}