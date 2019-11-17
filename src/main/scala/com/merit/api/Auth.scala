package com.merit.api

import akka.http.scaladsl.server.Directives.optionalHeaderValueByName
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.AuthorizationFailedRejection
import java.util.UUID
import com.merit.modules.users.UserID
import com.typesafe.config.ConfigFactory
import pdi.jwt.Jwt
import pdi.jwt.JwtClaim
import pdi.jwt.JwtAlgorithm
import java.time.Clock
import scala.util.Success
import scala.util.Failure
import io.circe.Decoder
import io.circe.parser._
import io.circe.generic.semiauto._

trait AuthDirectives {
  implicit val clock: Clock = Clock.systemUTC

  implicit val decoder: Decoder[UUID] = Decoder.forProduct1("userId")(UUID.fromString)
  val config                          = ConfigFactory.load().getConfig("jwt")
  val jwtSecret                       = config.getString("secret")

  def issueJwt(id: UserID): String =
    Jwt.encode(
      JwtClaim(s"""{ "userId": "${id.value}" }""").issuedNow.expiresIn(2629746),
      jwtSecret,
      JwtAlgorithm.HS256
    )

  def decodeJwt(token: String): Option[UserID] =
    Jwt
      .decode(token, jwtSecret, Seq(JwtAlgorithm.HS256))
      .toOption
      .flatMap(c => decode[UUID](c.content).map(UserID(_)).toOption)

  def authenticated: Directive1[UserID] =
    optionalHeaderValueByName("Authorization").flatMap {
      case None => reject(AuthorizationFailedRejection)
      case Some(header) =>
        decodeJwt(header) match {
          case None     => reject(AuthorizationFailedRejection)
          case Some(id) => provide(id)
        }
    }
}
