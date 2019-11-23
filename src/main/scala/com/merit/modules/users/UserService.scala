package com.merit.modules.users

import slick.dbio.DBIO
import slick.jdbc.PostgresProfile
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait UserService {
  def get(id: UserID): Future[Option[UserDTO]]
  def login(email: String, password: String): Future[Option[UserRow]]
  def register(email: String, name: String, password: String): Future[UserRow]
  def populateUsers: DBIO[UserRow]
}

object UserService {
  def hashPassword(pw: String): String =
    BCrypt.hashpw(pw, BCrypt.gensalt())

  def checkPassword(pw: String, hash: String): Boolean =
    BCrypt.checkpw(pw, hash)

  def apply(db: PostgresProfile.backend.Database, userRepo: UserRepo[DBIO])(
    implicit ec: ExecutionContext
  ): UserService =
    new UserService {
      def get(id: UserID): Future[Option[UserDTO]] =
        db.run(userRepo.get(id).map(_.map(UserDTO.fromRow(_))))

      def login(email: String, password: String): Future[Option[UserRow]] =
        db.run(userRepo.get(email)).map {
          case Some(user) if checkPassword(password, user.password) => Some(user)
          case _                                                    => None
        }

      def register(email: String, name: String, password: String): Future[UserRow] =
        db.run(userRepo.insert(UserRow(email, name, hashPassword(password))))

      def populateUsers: DBIO[UserRow] =
        userRepo.getAll.flatMap {
          case Seq() =>
            userRepo.insert(
              UserRow(
                "m@m.com",
                "mert",
                hashPassword("m"),
                UserID.from("74e7627b-1184-4125-b0fc-b478b7dfdf4e")
              )
            )
          case Seq(first) => DBIO.successful(first)
        }
    }
}
