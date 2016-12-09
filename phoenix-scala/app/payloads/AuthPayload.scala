package payloads

import cats.data.Xor
import failures._
import models.auth.Token
import org.jose4j.jwt.JwtClaims
import org.json4s.CustomSerializer
import org.json4s.jackson.{compactJson, parseJson}

case class AuthPayload(claims: JwtClaims, jwt: String)

object AuthPayload {

  /**
    * Requires valid PHOENIX_PRIVATE_KEY for encode JwtClaims
    */
  def apply(token: Token): Failures Xor AuthPayload = {
    val claims = Token.getJWTClaims(token)
    Token.encodeJWTClaims(claims).map { encoded ⇒
      AuthPayload(claims = claims, jwt = encoded)
    }
  }

  def jwt(token: Token): String = {
    val claims = Token.getJWTClaims(token)
    Token.encodeJWTClaims(claims).fold(f ⇒ "", a ⇒ a)
  }

  class JwtClaimsSerializerImpl
      extends CustomSerializer[JwtClaims](format ⇒
            ({
          case jv ⇒
            JwtClaims.parse(compactJson(jv))
        }, {
          case x: JwtClaims ⇒
            parseJson(x.toJson)
        }))

  val JwtClaimsSerializer = new JwtClaimsSerializerImpl
}
