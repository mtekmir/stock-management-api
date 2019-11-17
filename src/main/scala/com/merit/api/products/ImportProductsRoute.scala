package api

import com.merit.modules.brands.BrandService
import com.merit.modules.products.ProductService
import scala.concurrent.Await
import akka.http.scaladsl.server.Directives
import scala.concurrent.duration.Duration
import scala.util.Success
import com.merit.modules.excel.ExcelService
import scala.concurrent.ExecutionContext

object ImportRoute extends Directives with JsonSupport {
  def apply(
    brandService: BrandService,
    productService: ProductService,
    excelService: ExcelService
  )(implicit ec: ExecutionContext) =
    (path("import") & uploadedFile("file")) {
      case (metadata, file) => {
        val rows       = excelService.parseProductImportFile(file)
        val duplicates = excelService.validateProductRows(rows)

        if (duplicates.length > 0) {
          complete(UnSuccessfulProductImport("File contains duplicates", duplicates))
        } else {
          val ids = for {
            _   <- brandService.batchInsertExcelRows(rows)
            ids <- productService.batchInsertExcelRows(rows)
          } yield ids
          complete(ids)
        }
      }
    }
}
