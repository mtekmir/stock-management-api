package api

import com.merit.modules.users.UserID
import java.util.UUID
import org.joda.time.DateTime
import com.merit.modules.products.{Currency,ProductDTO}
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID
import com.merit.modules.inventoryCount.InventoryCountProductID
import com.merit.modules.sales.PaymentMethod
case class LoginRequest(
  email: String,
  password: String
)

case class CreateSaleRequest(
  total: Currency,
  discount: Currency,
  description: Option[String],
  paymentMethod: Option[PaymentMethod.Value],
  products: Seq[ProductDTO]
)

case class CreateInventoryCountRequest(
  startDate: Option[DateTime],
  name: Option[String],
  brandId: Option[BrandID],
  categoryId: Option[CategoryID]
)

case class CountInventoryCountProductRequest(
  id: InventoryCountProductID,
  count: Int
)