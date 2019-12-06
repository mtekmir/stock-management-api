package com.merit.modules.emails

import akka.actor.Actor
import akka.actor.ActorLogging
import org.apache.commons.mail.{SimpleEmail, Email}
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext

class EmailServiceActor(emailSettings: EmailSettings) extends Actor with ActorLogging {
  implicit val ec: ExecutionContext =
    context.system.dispatchers.lookup("email-service-dispatcher")
  private var attempts: Int    = 0
  private val maxAttempts: Int = 5
  private def configure(email: Email): Email = {
    import emailSettings._
    email.setHostName(host)
    email.setAuthentication(username, password)
    email.setSSLOnConnect(true)
    email.setSmtpPort(port)
    email
  }

  private def sendSimpleEmail(eM: EmailMessage) = {
    import eM._
    val email = new SimpleEmail()
    email.setFrom("merit.stocks@gmail.com")
    email.setSubject(subject)
    email.setMsg(content)
    email.addTo(recipient)

    log.debug("Atempting to deliver message")

    configure(email).send()
  }

  def receive: Receive = {
    case email: EmailMessage if email.attachment.isDefined =>
    case email: EmailMessage =>
      try {
        sendSimpleEmail(email)
      } catch {
        case e: Exception =>
          if (attempts < maxAttempts) {
            attempts += 1
            context.system.scheduler.scheduleOnce(10.seconds, self, email)
          } else {
            // failed forever
            log.warning("Failed to send an email to {}", email.recipient)
          }
          throw e
      }

    case unexpectedMessage: Any => {
      log.warning("Received unexepected message : {}", unexpectedMessage)
    }
  }
}
