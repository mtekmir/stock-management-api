package api

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.AutoDerivation
import org.joda.time.DateTime
import io.circe.Json
import io.circe.Encoder
import io.circe.Decoder
import com.merit.modules.sales.{SaleID, SaleOutlet}
import com.merit.modules.products.ProductID
import com.merit.modules.brands.BrandID
import com.merit.modules.users.UserID
import com.merit.modules.excel.ValidationErrorTypes

import org.joda.time.format.DateTimeFormat
import com.merit.modules.products.Currency
import cats.syntax.either._
import com.merit.modules.stockOrders.StockOrderID
import com.merit.modules.inventoryCount.{InventoryCountBatchID, InventoryCountProductID, InventoryCountStatus}

trait JsonSupport extends FailFastCirceSupport with AutoDerivation {
  implicit val encodeDT: Encoder[DateTime] = (d: DateTime) =>
    Encoder.encodeString(d.toString("yyyy-MM-d"))

  val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  implicit val decodeDateTime: Decoder[DateTime] =
    Decoder.instance(d => d.as[String].map(s => DateTime.parse(s, dateFormatter)))

  implicit val encodeSaleId: Encoder[SaleID] = (id: SaleID) => Encoder.encodeLong(id.value)
  implicit val decodeSaleId: Decoder[SaleID] = Decoder.decodeLong.emap { v =>
    SaleID(v).asRight
  }
  implicit val encodeStockOrderId: Encoder[StockOrderID] = (id: StockOrderID) =>
    Encoder.encodeLong(id.value)
  implicit val decodeStockOrderId: Decoder[StockOrderID] = Decoder.decodeLong.emap { v =>
    StockOrderID(v).asRight
  }

  implicit val encodeProductId: Encoder[ProductID] = (id: ProductID) =>
    Encoder.encodeLong(id.value)
  implicit val decodeProductId: Decoder[ProductID] = Decoder.decodeLong.emap { v =>
    ProductID(v).asRight
  }

  implicit val encodeBrandId: Encoder[BrandID] = (id: BrandID) => Encoder.encodeLong(id.value)
  implicit val decodeBrandId: Decoder[BrandID] = Decoder.decodeLong.emap { v =>
    BrandID(v).asRight
  }

  implicit val encodeUserId: Encoder[UserID] = (id: UserID) => Encoder.encodeUUID(id.value)
  implicit val decodeUserId: Decoder[UserID] = Decoder.decodeUUID.emap { v =>
    UserID(v).asRight
  }

  implicit val encodeInventoryCountBatchId: Encoder[InventoryCountBatchID] =
    (id: InventoryCountBatchID) => Encoder.encodeLong(id.value)
  implicit val decodeInventoryCountBatchId: Decoder[InventoryCountBatchID] =
    Decoder.decodeLong.emap { v =>
      InventoryCountBatchID(v).asRight
    }

  implicit val encodeInventoryCountProductId: Encoder[InventoryCountProductID] =
    (id: InventoryCountProductID) => Encoder.encodeLong(id.value)
  implicit val decodeInventoryCountProductId: Decoder[InventoryCountProductID] =
    Decoder.decodeLong.emap { v =>
      InventoryCountProductID(v).asRight
    }

  implicit val encodeCurrency: Encoder[Currency] = (currency: Currency) =>
    Encoder.encodeBigDecimal(currency.value)

  implicit val decodeCurrency: Decoder[Currency] = Decoder.decodeBigDecimal.emap { v =>
    Currency(v).asRight
  }

  implicit val encodeErrorType: Encoder[ValidationErrorTypes.Value] =
    Encoder.enumEncoder(ValidationErrorTypes)

  implicit val encodeInventoryCountStatus: Encoder[InventoryCountStatus.Value] =
    Encoder.enumEncoder(InventoryCountStatus)

  implicit val encodeSaleOutlet: Encoder[SaleOutlet.Value] =
    Encoder.enumEncoder(SaleOutlet)

  implicit val decodeSaleOutlet: Decoder[SaleOutlet.Value] =
    Decoder.enumDecoder(SaleOutlet)
}
