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

case class JwtConfig(
  secret: String
)

case class HttpConfig(
  interface: String,
  port: Int
)

case class AppConfig(
  environment: String,
  aWSConfig: AwsConfig,
  dbConfig: DbConfig,
  emailConfig: EmailConfig,
  crawlerClientConfig: CrawlerClientConfig,
  jwtConfig: JwtConfig,
  httpConfig: HttpConfig
)

trait Config {
  def load(): AppConfig
}

object Config {
  def apply() = new Config {
    private val environment        = sys.env.getOrElse("PROJECT_ENV", "local")
    private val port               = sys.env.get("PORT")
    private val config             = ConfigFactory.load
    private val localDbConfig      = loadConfigOrThrow[DbConfig](config, "db")
    private val emailConfig        = loadConfigOrThrow[EmailConfig](config, "email")
    private val aWSConfig          = loadConfigOrThrow[AwsConfig](config, "aws")
    private val httpConfig         = loadConfigOrThrow[HttpConfig](config, "http")
    private val parameterUrlPrefix = s"/stock-management-service/$environment/"

    private val sSM = SsmClient.create()

    private def getParam(name: String): Try[String] =
      Try(
        sSM
          .getParameter(
            GetParameterRequest
              .builder()
              .name(s"${parameterUrlPrefix}$name")
              .withDecryption(true)
              .build()
          )
      ).map(_.parameter().value())

    def getParameter(name: String): String = getParam(name).get
    def getParameterOrElse(name: String, default: String): String =
      getParam(name).getOrElse(default)

    val dbConfig = environment match {
      case "local" => localDbConfig
      case _ =>
        localDbConfig.copy(
          url = getParameter("jdbc-url"),
          username = getParameter("db-username"),
          password = getParameter("db-password")
        )
    }

    val crawlerClientConfig = environment match {
      case "local" => CrawlerClientConfig("", "", "")
      case _ =>
        CrawlerClientConfig(
          queueUrl = getParameter("crawler-queue-url"),
          username = getParameterOrElse("crawler-username", "u"),
          password = getParameterOrElse("crawler-password", "p")
        )
    }

    val jwtConfig = JwtConfig(secret = getParameterOrElse("jwt-secret", "secret"))

    def load() = AppConfig(
      environment,
      aWSConfig,
      dbConfig,
      emailConfig,
      crawlerClientConfig,
      jwtConfig,
      httpConfig.copy(port = port.map(_.toInt).getOrElse(3111))
    )
  }
}
