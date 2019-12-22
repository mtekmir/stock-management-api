package api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.sales.SaleService
import akka.http.scaladsl.server.Route
import com.merit.modules.sales.SaleID
import akka.http.scaladsl.server.PathMatcher1
import scala.concurrent.ExecutionContext
import scala.util.Success
import akka.http.scaladsl.model.StatusCodes
import scala.util.Failure


object GetSale extends Directives with JsonSupport {
  val saleIdMatcher: PathMatcher1[SaleID] = LongNumber.map(SaleID(_))
  def apply(saleService: SaleService)(implicit ec: ExecutionContext): Route = {
    (get & path(saleIdMatcher)) { id => 
      onComplete(saleService.getSale(id)) {
        case Success(Some(sale)) => complete(sale)
        case Success(None) => complete(StatusCodes.NotFound)
        case Failure(_) => complete(StatusCodes.InternalServerError)
      }
    }
  }
}