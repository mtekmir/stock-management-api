package com.merit.external.sqsClient

import collection.JavaConverters._
import software.amazon.awssdk.services.sqs.{SqsClient => Sqs}
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import com.merit.AwsConfig
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import com.merit.external.crawler.MessageType

trait SqsClient {
  def sendMessageTo(
    queueUrl: String,
    messageType: MessageType.Value,
    message: String
  ): SendMessageResponse
}

object SqsClient {
  def apply(config: AwsConfig) = new SqsClient {
    private val client = Sqs.create()

    def sendMessageTo(
      queueUrl: String,
      messageType: MessageType.Value,
      message: String
    ): SendMessageResponse =
      client.sendMessage(
        SendMessageRequest
          .builder()
          .queueUrl(queueUrl)
          .messageBody(message)
          .messageAttributes(
            Map(
              (
                "message_type",
                MessageAttributeValue
                  .builder()
                  .dataType("String")
                  .stringValue(messageType.toString)
                  .build()
              )
            ).asJava
          )
          .build()
      )
  }
}
