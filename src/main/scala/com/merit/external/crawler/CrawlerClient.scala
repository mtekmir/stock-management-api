package com.merit.external.crawler

import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import com.merit.modules.sales.{SaleSummary, SaleSummaryProduct}
import com.merit.external.sqsClient.SqsClient
import scala.concurrent.{ExecutionContext, Future}
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import com.merit.modules.stockOrders.{StockOrderSummary, StockOrderSummaryProduct}
import com.merit.CrawlerClientConfig
import com.merit.modules.inventoryCount.{InventoryCountDTO, InventoryCountProductDTO}
import com.merit.modules.products.ProductID

trait CrawlerClient {
  def sendSale(sale: SaleSummary): Future[(SyncSaleMessage, SendMessageResponse)]
  def sendStockOrder(
    stockOrder: StockOrderSummary
  ): Future[(SyncStockOrderMessage, SendMessageResponse)]
  def sendInventoryCount(
    inventoryCount: InventoryCountDTO,
    products: Seq[InventoryCountProductDTO]
  ): Future[(Seq[SyncInventoryCountMessage], Seq[SendMessageResponse])]
}

object CrawlerClient {
  def apply(config: CrawlerClientConfig, client: SqsClient)(
    implicit ec: ExecutionContext
  ): CrawlerClient =
    new CrawlerClient with CrawlerCodec {
      def sendSale(
        saleSummary: SaleSummary
      ): Future[(SyncSaleMessage, SendMessageResponse)] = {
        val message =
          SyncSaleMessage(
            saleSummary.id,
            saleSummary.products.map {
              case SaleSummaryProduct(id, barcode, _, _, prevQty, soldQty) =>
                SyncMessageProduct(id, barcode, soldQty)
            }
          )
        Future {
          (
            message,
            client.sendMessageTo(
              config.queueUrl,
              MessageType.Sale,
              encodeSaleMessage(message).toString
            )
          )
        }
      }

      def sendStockOrder(
        stockOrder: StockOrderSummary
      ): Future[(SyncStockOrderMessage, SendMessageResponse)] = {
        val message =
          SyncStockOrderMessage(
            stockOrder.id,
            stockOrder.updated.map {
              case StockOrderSummaryProduct(id, barcode, _, _, prevQty, orderedQty) => {
                SyncMessageProduct(id, barcode, orderedQty)
              }
            }
          )

        Future {
          (
            message,
            client.sendMessageTo(
              config.queueUrl,
              MessageType.StockOrder,
              encodeStockOrderMessage(message).toString
            )
          )
        }
      }

      def sendInventoryCount(
        inventoryCount: InventoryCountDTO,
        products: Seq[InventoryCountProductDTO]
      ): Future[(Seq[SyncInventoryCountMessage], Seq[SendMessageResponse])] = {
        val messages = products
          .sliding(100, 100)
          .map(
            products =>
              SyncInventoryCountMessage(
                inventoryCount.id,
                products = products.map(
                  p =>
                    SyncInventoryCountProduct(
                      p.id,
                      p.barcode,
                      p.counted.getOrElse(0)
                    )
                )
              )
          )
          .toSeq

        val responses = messages.map(
          m =>
            client.sendMessageTo(
              config.queueUrl,
              MessageType.InventoryCount,
              encodeInventoryCountMessage(m).toString()
            )
        )

        Future.successful { (messages, responses) }
      }

    }
}
