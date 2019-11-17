package com.merit.api.users
import akka.http.scaladsl.server.Directives
import com.merit.api.AuthDirectives
import api.JsonSupport
import akka.http.scaladsl.server.Route
import com.merit.modules.users.UserService
import api.LoginRequest
import akka.http.scaladsl.server.AuthorizationFailedRejection
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes._

object LoginRoute extends Directives with AuthDirectives with JsonSupport {
  def apply(userService: UserService): Route =
    (path("login") & entity(as[LoginRequest])) { loginReq =>
      onComplete(userService.login(loginReq.email, loginReq.password)) {
        case Success(Some(user)) => complete(issueJwt(user.id))
        case Success(None)       => complete(AuthorizationFailedRejection)
        case Failure(e) =>
          println(e)
          complete(InternalServerError -> "Something went wrong")
      }
    }
}
