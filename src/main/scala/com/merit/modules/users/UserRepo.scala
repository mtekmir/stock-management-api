package com.merit.modules.users

import db.Schema

trait UserRepo[DbTask[_]] {
  def get(id: UserID): DbTask[Option[UserRow]]
  def get(email: String): DbTask[Option[UserRow]]
  def getAll: DbTask[Seq[UserRow]]
  def insert(user: UserRow): DbTask[UserRow]
}

object UserRepo {
  def apply(schema: Schema) = new UserRepo[slick.dbio.DBIO] {
    import schema._
    import schema.profile.api._

    def get(id: UserID): DBIO[Option[UserRow]] =
      users.filter(_.id === id).result.headOption
    

    def get(email: String): DBIO[Option[UserRow]] =
      users.filter(_.email === email).result.headOption

    def getAll: DBIO[Seq[UserRow]] = 
      users.result

    def insert(user: UserRow): DBIO[UserRow] =
      users returning users += user
  }
}
