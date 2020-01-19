package com.merit.external.crawler

import cats.syntax.either._
import io.circe.Encoder
import com.merit.modules.sales.SaleID
import io.circe.Decoder
import com.merit.modules.products.ProductID
import com.merit.modules.inventoryCount.InventoryCountBatchID

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

  implicit val encodeInventoryCountBatchId: Encoder[InventoryCountBatchID] =
    (id: InventoryCountBatchID) => Encoder.encodeLong(id.value)
  implicit val decodeInventoryCountBatchId: Decoder[InventoryCountBatchID] =
    Decoder.decodeLong.emap { v =>
      InventoryCountBatchID(v).asRight
    }

  implicit val encodeProduct: Encoder[SyncMessageProduct] =
    Encoder.forProduct3("id", "barcode", "qty")(
      p => (p.id, p.barcode, p.qty)
    )

  def encodeSaleMessage: Encoder[SyncSaleMessage] =
    Encoder.forProduct2("saleId", "products")(
      m => (m.saleId.value, m.products)
    )

  def encodeStockOrderMessage: Encoder[SyncStockOrderMessage] =
    Encoder.forProduct2("stockOrderId", "products")(
      m => (m.stockOrderId.value, m.products)
    )

  def encodeInventoryCountMessage: Encoder[SyncInventoryCountMessage] =
    Encoder.forProduct2("inventoryCountBatchId", "products")(
      m => (m.inventoryCountBatchId, m.products)
    )
}
