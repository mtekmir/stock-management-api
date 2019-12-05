package api.sales

import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.sales.SaleService
import com.merit.modules.products.ProductService
import scala.concurrent.ExecutionContext
import com.merit.modules.excel.ExcelService
import api.ImportSaleRequest
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import akka.http.scaladsl.model.StatusCodes.BadRequest

object ImportSaleRoute extends Directives with JsonSupport {
  def apply(
    saleService: SaleService,
    productService: ProductService,
    excelService: ExcelService
  )(implicit ec: ExecutionContext) =
    (path("import") & formFields('date.as[String]) & uploadedFile("file")) {
      case (date, (metadata, file)) =>
        val rows          = excelService.parseSaleImportFile(file)
        val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

        rows match {
          case Left(error) => complete(BadRequest -> error)
          case Right(rows) =>
            complete(
              saleService.insertFromExcel(rows, DateTime.parse(date, dateFormatter))
            )
        }

    }
}
