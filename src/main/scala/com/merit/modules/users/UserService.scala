package com.merit.modules.users

import slick.dbio.DBIO
import slick.jdbc.PostgresProfile
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database
import com.typesafe.scalalogging.LazyLogging

trait UserService {
  def get(id: UserID): Future[Option[UserDTO]]
  def login(email: String, password: String): Future[Option[UserRow]]
  def register(email: String, name: String, password: String): Future[UserRow]
}

object UserService {
  def hashPassword(pw: String): String =
    BCrypt.hashpw(pw, BCrypt.gensalt())

  def checkPassword(pw: String, hash: String): Boolean =
    BCrypt.checkpw(pw, hash)

  def apply(db: Database, userRepo: UserRepo[DBIO])(
    implicit ec: ExecutionContext
  ): UserService =
    new UserService with LazyLogging {
      def get(id: UserID): Future[Option[UserDTO]] =
        db.run(userRepo.get(id).map(_.map(UserDTO.fromRow(_))))

      def login(email: String, password: String): Future[Option[UserRow]] =
        db.run(userRepo.get(email)).map {
          case Some(user) if checkPassword(password, user.password) =>
            logger.info(s"Login successful with email $email")
            Some(user)
          case _ =>
            logger.warn(s"Login unsuccessful with email $email")
            None
        }

      def register(email: String, name: String, password: String): Future[UserRow] = {
        logger.info(s"Registering user with email")
        db.run(userRepo.insert(UserRow(email, name, hashPassword(password))))
      }
    }
}
