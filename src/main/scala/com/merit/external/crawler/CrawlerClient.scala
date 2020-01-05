package com.merit.external.crawler

import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import com.merit.modules.sales.SaleSummary
import com.merit.modules.sales.SaleSummaryProduct
import com.merit.external.sqsClient.SqsClient
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import com.merit.modules.stockOrders.StockOrderSummary
import com.merit.modules.stockOrders.StockOrderSummaryProduct
import com.merit.CrawlerClientConfig

trait CrawlerClient {
  def sendSale(sale: SaleSummary): Future[(SyncSaleMessage, SendMessageResponse)]
  def sendStockOrder(
    stockOrder: StockOrderSummary
  ): Future[(SyncStockOrderMessage, SendMessageResponse)]
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
              encodeSaleMessage(message).toString()
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
              encodeStockOrderMessage(message).toString()
            )
          )
        }
      }

    }
}
