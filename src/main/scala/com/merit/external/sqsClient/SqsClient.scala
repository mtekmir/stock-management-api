package com.merit.external.sqsClient

import software.amazon.awssdk.services.sqs.{SqsClient => Sqs}
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import com.merit.AwsConfig

trait SqsClient {
  def sendMessageTo(queueUrl: String, message: String): SendMessageResponse
}

object SqsClient {
  def apply(config: AwsConfig) = new SqsClient {
    private val client = Sqs
      .builder()
      .region(Region.EU_CENTRAL_1)
      .credentialsProvider(
        ProfileCredentialsProvider
          .builder()
          .profileName(config.profile)
          .build()
      )
      .build()

    def sendMessageTo(queueUrl: String, message: String): SendMessageResponse =
      client.sendMessage(
        SendMessageRequest
          .builder()
          .queueUrl(queueUrl)
          .messageBody(message)
          .build()
      )
  }
}
