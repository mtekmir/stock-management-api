package api

import com.merit.modules.users.UserID
import java.util.UUID
import org.joda.time.DateTime
import com.merit.modules.products.{Currency,ProductDTO}
case class LoginRequest(
  email: String,
  password: String
)

case class CreateSaleRequest(
  total: Currency,
  discount: Currency,
  products: Seq[ProductDTO]
)
