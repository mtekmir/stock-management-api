package api

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.AutoDerivation
import org.joda.time.DateTime
import io.circe.Json
import io.circe.Encoder
import io.circe.Decoder
import com.merit.modules.sales.SaleID
import com.merit.modules.products.ProductID
import com.merit.modules.brands.BrandID
import com.merit.modules.users.UserID

import org.joda.time.format.DateTimeFormat

trait JsonSupport extends FailFastCirceSupport with AutoDerivation {
  implicit val encodeDT: Encoder[DateTime] = (d: DateTime) =>
    Encoder.encodeString(d.toString("yyyy-MM-d"))

  val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  implicit val decodeDateTime: Decoder[DateTime] =
    Decoder.instance(d => d.as[String].map(s => DateTime.parse(s, dateFormatter)))

  implicit val encodeSaleId: Encoder[SaleID] = (id: SaleID) =>
    Encoder.encodeLong(id.value)
    
  implicit val encodeProductId: Encoder[ProductID] = (id: ProductID) =>
    Encoder.encodeLong(id.value)

  implicit val encodeBrandId: Encoder[BrandID] = (id: BrandID) =>
    Encoder.encodeLong(id.value)

  implicit val encodeUserId: Encoder[UserID] = (id: UserID) =>
    Encoder.encodeUUID(id.value)
}
