package api

import com.merit.modules.excel.DuplicateBarcode
import com.merit.modules.products.ProductID
import com.merit.modules.users.UserID
import java.util.UUID
import org.joda.time.DateTime

sealed trait Response {
  val message: String
}

case class UnSuccessfulProductImport(
  message: String,
  duplicates: Seq[DuplicateBarcode]
) extends Response

case class SuccessfulProductImport(
  message: String,
  imported: Seq[ProductID]
) extends Response

case class SaleImportResponse(
  message: String
) extends Response

case class LoginRequest(
  email: String,
  password: String
)

case class ImportSaleRequest(
  date: DateTime
)
