package models.auth

import akka.http.scaladsl.model.headers.{
  BasicHttpCredentials,
  GenericHttpCredentials,
  HttpCredentials
}

import cats.data.Xor
import failures.Failures
import utils.db._

sealed trait Credentials {
  val secret: String
}

case class JWTCredentials(secret: String)         extends Credentials
case class BearerTokenCredentials(secret: String) extends Credentials

object Credentials {

  def mustVerifyJWTCredentials(cred: Option[HttpCredentials],
                               or: Failures): Failures Xor JWTCredentials =
    cred.flatMap {
      // assume it's JWT
      // passing scheme as argument where we expect token is not a typo
      case GenericHttpCredentials(scheme, token, params) ⇒ Some(JWTCredentials(scheme))
      case _                                             ⇒ None
    }.toXor(or)
}
