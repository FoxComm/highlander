package models.auth

import scala.concurrent.{Future, ExecutionContext}

import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsRejected, CredentialsMissing}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1}
import akka.http.scaladsl.server.directives.AuthenticationDirective
import com.softwaremill.session.{InMemoryRefreshTokenStorage, SessionManager, JwtSessionEncoder,
JValueSessionSerializer, SessionConfig}
import akka.http.scaladsl.model.headers.GenericHttpCredentials
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import models.StoreAdmin
import models.customer.{Customers, Customer}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import akka.http.scaladsl.server.Directives._


object Session {
  val config: com.typesafe.config.Config = utils.Config.loadWithEnv()
  val sessionConfig = SessionConfig.default(config.getString("auth.secretKey"))

  val adminSession = new Session[AdminToken, StoreAdmin]
  val customerSession = new Session[CustomerToken, Customer]
  import services.Authenticator.AsyncAuthenticator

  def requireAdminAuth: Directive1[StoreAdmin] = {
    extractExecutionContext.flatMap { implicit ec ⇒
      adminSession.optionalSessionT(token ⇒ Future.successful(Some(StoreAdmin(id = token.id,
              email = token.email,
              hashedPassword = None,
              name = token.name.getOrElse(""),
              department = token.department
        ))))
    }
  }

  def requireCustomerAuth(implicit db: Database): Directive1[Customer] = {
    extractExecutionContext.flatMap { implicit ec ⇒
      customerSession.optionalSessionT(token ⇒ {
        Customers.findOneById(token.id).run()
      })
    }
  }

  def requireJwtAuth[T](header: String, auth: AsyncAuthenticator[T]): AuthenticationDirective[T] = {
    optionalHeaderValueByName(header).map(_.map { token ⇒
        GenericHttpCredentials(token, "")
      }).flatMap { optCreds ⇒
        onSuccess(auth(optCreds)).flatMap {
          case Right(user) ⇒ provide(user)
          case Left(challenge) ⇒
            val cause = if (optCreds.isEmpty) CredentialsMissing else CredentialsRejected
            reject(AuthenticationFailedRejection(cause, challenge)): Directive1[T]
        }
      }
  }


  def setTokenSession(token: Token) = {
    extractExecutionContext.flatMap { implicit ec ⇒
      token match {
        case t: AdminToken ⇒ adminSession.setSessionT(t)
        case t: CustomerToken ⇒ customerSession.setSessionT(t)
      }
    }
  }

  def setCustomerSession(token: CustomerToken)(implicit ec: ExecutionContext) = customerSession.setSessionT(token)
}


final class Session[T <: Product: Manifest, M <: Product] {
  import Session._

  implicit val serializer = JValueSessionSerializer.caseClass[T]
  implicit val encoder = new JwtSessionEncoder[T]
  implicit val manager = new SessionManager[T](sessionConfig)

  implicit val refreshTokenStorage = new InMemoryRefreshTokenStorage[T] {
    def log(msg: String) = System.err.println(msg)
  }

  def optionalSessionT(f: T ⇒ Future[Option[M]])(implicit ec: ExecutionContext): Directive1[M] = {
    optionalSession(refreshable, usingHeaders).flatMap {
      case Some(token) ⇒ onSuccess(f(token)).flatMap {
        case Some(model) ⇒ provide(model)
        case None ⇒ reject(refreshable.clientSessionManager.sessionMissingRejection)
      }
      case None ⇒ reject(refreshable.clientSessionManager.sessionMissingRejection)
    }
  }

  def requireSessionT(implicit ec: ExecutionContext) = requiredSession(refreshable, usingHeaders)
  def setSessionT(session: T)(implicit ec: ExecutionContext) = setSession(refreshable, usingHeaders, session)
}