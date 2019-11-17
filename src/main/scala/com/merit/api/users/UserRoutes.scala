package com.merit.api.users
import akka.http.scaladsl.server.Directives
import com.merit.api.AuthDirectives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import scala.concurrent.ExecutionContext
import com.merit.modules.users.{UserID, UserService}

object UserRoutes extends Directives with AuthDirectives with JsonSupport {
  def apply(userId: UserID, userService: UserService)(
    implicit ec: ExecutionContext
  ): Route =
    MeRoute(userId, userService)
}
