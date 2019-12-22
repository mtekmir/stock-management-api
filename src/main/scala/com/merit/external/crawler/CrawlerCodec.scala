package com.merit.external.crawler

import cats.syntax.either._
import io.circe.Encoder
import com.merit.modules.sales.SaleID
import io.circe.Decoder
import com.merit.modules.products.ProductID

trait CrawlerCodec {
  implicit val encodeSaleId: Encoder[SaleID] = (id: SaleID) => Encoder.encodeLong(id.value)
  implicit val decodeSaleId: Decoder[SaleID] = Decoder.decodeLong.emap { v =>
    SaleID(v).asRight
  }

  implicit val encodeProductId: Encoder[ProductID] = (id: ProductID) =>
    Encoder.encodeLong(id.value)
  implicit val decodeProductId: Decoder[ProductID] = Decoder.decodeLong.emap { v =>
    ProductID(v).asRight
  }
  
  implicit val encodeErrorType: Encoder[AdjustmentType.Value] =
    Encoder.enumEncoder(AdjustmentType)

  implicit val encodeProduct: Encoder[SyncMessageProduct] =
    Encoder.forProduct4("id", "barcode", "qty", "adjustmentType")(
      p => (p.id, p.barcode, p.qty.toString, p.adjustmentType)
    )

  def encodeSaleMessage: Encoder[SyncSaleMessage] =
    Encoder.forProduct2("saleId", "products")(
      m => (m.saleId.value, m.products)
    )

  def encodeStockOrderMessage: Encoder[SyncStockOrderMessage] =
    Encoder.forProduct2("stockOrderId", "products")(
      m => (m.stockOrderId.value, m.products)
    )
}
