package com.merit.api.inventoryCount

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import akka.http.scaladsl.server.{Route, PathMatcher1}
import com.merit.modules.inventoryCount.InventoryCountService
import com.merit.modules.inventoryCount.InventoryCountBatchID
import scala.concurrent.ExecutionContext
import scala.util.Failure
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.scalalogging.LazyLogging
import scala.util.Success

object GetInventoryCountBatch extends Directives with JsonSupport with LazyLogging {
  def apply(
    inventoryCountService: InventoryCountService
  )(implicit ec: ExecutionContext): Route = {
    val batchIdMatcher: PathMatcher1[InventoryCountBatchID] =
      LongNumber.map(InventoryCountBatchID(_))
    path(batchIdMatcher) { batchId =>
      onComplete(inventoryCountService.getBatch(batchId)) {
        case Failure(exception) =>
          logger.warn(exception.getMessage())
          complete(StatusCodes.InternalServerError -> "Something went wrong")
        case Success(None)    => complete(StatusCodes.NotFound)
        case Success(Some(b)) => complete(b)
      }
    }
  }
}
