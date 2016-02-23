package models.auth

import akka.http.scaladsl.model.headers.{BasicHttpCredentials, GenericHttpCredentials, HttpCredentials}

import cats.data.Xor
import services.Failures
import utils.DbResultT.implicits._

sealed trait Credentials {
  val secret: String
}

final case class BasicCredentials(identifier: String, secret: String) extends Credentials
final case class JWTCredentials(secret: String) extends Credentials
final case class BearerTokenCredentials(secret: String) extends Credentials

object Credentials {

  def mustBasicOr(cred: Option[HttpCredentials], or: Failures): Failures Xor BasicCredentials = (cred match {
    case Some(BasicHttpCredentials(username, secret)) ⇒ Some(BasicCredentials(username, secret))
    case _ ⇒ None
  }).toXor(or)

  def mustJwtOr(cred: Option[HttpCredentials], or: Failures): Failures Xor JWTCredentials = (cred match {
    // assume it's JWT, also it's not a typo - token contained in scheme for GenericHttpCredentials
    case Some(GenericHttpCredentials(scheme, token, params)) ⇒ Some(JWTCredentials(scheme))
    case _ ⇒ None
  }).toXor(or)
}
