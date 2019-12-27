package com.merit

import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.generic.auto._
import scala.util.Try
import software.amazon.awssdk.services.ssm.model.GetParametersRequest

case class AwsConfig(
  profile: String
)

case class DbConfig(
  username: String,
  password: String,
  url: String,
  driver: String,
  maxPoolSize: Int,
  numThreads: Int
)

case class EmailConfig(
  host: String,
  port: Int,
  username: String,
  password: String
)

case class CrawlerClientConfig(
  queueUrl: String,
  username: String,
  password: String
)

case class AppConfig(
  aWSConfig: AwsConfig,
  dbConfig: DbConfig,
  emailConfig: EmailConfig,
  crawlerClientConfig: CrawlerClientConfig
)

trait Config {
  def load(): AppConfig
}

object Config {
  def apply() = new Config {
    private val config              = ConfigFactory.load
    private val environment         = config.getString("environment")
    private val localDbConfig       = loadConfigOrThrow[DbConfig](config, "db")
    private val emailConfig         = loadConfigOrThrow[EmailConfig](config, "email")
    private val crawlerClientConfig = loadConfigOrThrow[CrawlerClientConfig](config, "crawler")
    private val aWSConfig           = loadConfigOrThrow[AwsConfig](config, "aws")
    private val parameterUrlPrefix  = s"/stock-management-service/$environment/"

    val sSM = environment match {
      case "production" => SsmClient.create()
      case _ =>
        SsmClient
          .builder()
          .region(Region.EU_CENTRAL_1)
          .credentialsProvider(
            ProfileCredentialsProvider
              .builder()
              .profileName(aWSConfig.profile)
              .build()
          )
          .build()
    }

    def getParameter(name: String) =
      Try(
        sSM
          .getParameter(
            GetParameterRequest
              .builder()
              .name(s"${parameterUrlPrefix}$name")
              .withDecryption(true)
              .build()
          )
      ).map(_.parameter().value()).get

    val dbConfig = environment match {
      case "production" =>
        localDbConfig.copy(
          url = getParameter("jdbc-url"),
          username = getParameter("db-username"),
          password = getParameter("db-password")
        )
      case _ => localDbConfig
    }

    def load() = AppConfig(
      aWSConfig,
      dbConfig,
      emailConfig,
      crawlerClientConfig
    )
  }
}
