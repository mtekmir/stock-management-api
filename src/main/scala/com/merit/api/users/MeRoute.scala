package com.merit.api.users
import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.users.{UserService, UserID}
import akka.http.scaladsl.server.Route
import scala.concurrent.ExecutionContext
import scala.util.Success
import akka.http.scaladsl.model.StatusCodes._
import scala.util.Failure

object MeRoute extends Directives with JsonSupport {
  def apply(userId: UserID, userService: UserService)(
    implicit ec: ExecutionContext
  ): Route =
    (path("me") & get) {
      onComplete(userService.get(userId)) {
        case Success(Some(user)) => complete(user)
        case Success(None)       => complete(NotFound -> "User not found")
        case Failure(exception) =>
          println(exception)
          complete(InternalServerError -> "Something went wrong")
      }
    }
}
