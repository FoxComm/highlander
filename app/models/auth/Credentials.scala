package models.auth

import akka.http.scaladsl.model.headers.{BasicHttpCredentials, GenericHttpCredentials, HttpCredentials}

import cats.data.Xor
import failures.Failures
import utils.DbResultT.implicits._

sealed trait Credentials {
  val secret: String
}

final case class BasicCredentials(identifier: String, secret: String) extends Credentials
final case class JWTCredentials(secret: String) extends Credentials
final case class BearerTokenCredentials(secret: String) extends Credentials

object Credentials {

  def mustVerifyBasicCredentials(cred: Option[HttpCredentials], or: Failures): Failures Xor BasicCredentials = cred.flatMap {
    case BasicHttpCredentials(username, secret) ⇒ Some(BasicCredentials(username, secret))
    case _ ⇒ None
  }.toXor(or)

  def mustVerifyJWTCredentials(cred: Option[HttpCredentials], or: Failures): Failures Xor JWTCredentials = cred.flatMap {
    // assume it's JWT
    // passing scheme as argument where we expect token is not a typo
    case GenericHttpCredentials(scheme, token, params) ⇒ Some(JWTCredentials(scheme))
    case _ ⇒ None
  }.toXor(or)
}
