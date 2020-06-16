package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.inventoryCount.InventoryCountService
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.PathMatcher1
import com.merit.modules.inventoryCount.InventoryCountBatchID
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.scalalogging.LazyLogging
import scala.util.Success

object CompleteInventoryCount extends Directives with JsonSupport with LazyLogging {
  def apply(inventoryCountService: InventoryCountService): Route = {
    val batchIdMatcher: PathMatcher1[InventoryCountBatchID] =
      LongNumber.map(InventoryCountBatchID(_))

    (post & path(batchIdMatcher / "complete") & parameter('force ? false)) {
      (batchId, force) =>
        onComplete(inventoryCountService.complete(batchId, force)) {
          case Failure(exception) =>
            logger.warn(exception.getMessage())
            complete(StatusCodes.InternalServerError)
          case Success(Left(message)) => complete(StatusCodes.BadRequest -> message)
          case Success(Right(res))    => complete(res)
        }
    }
  }
}
