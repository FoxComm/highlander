package payloads

import cats.implicits._
import failures._
import io.circe.jackson.syntax._
import io.circe.parser.parse
import io.circe.{Decoder, Encoder, Json}
import models.auth.Token
import org.jose4j.jwt.JwtClaims

case class AuthPayload(claims: JwtClaims, jwt: String)

object AuthPayload {

  /**
    * Requires valid PHOENIX_PRIVATE_KEY for encode JwtClaims
    */
  def apply(token: Token): Either[Failures, AuthPayload] = {
    val claims = Token.getJWTClaims(token)
    Token.encodeJWTClaims(claims).map { encoded ⇒
      AuthPayload(claims = claims, jwt = encoded)
    }
  }

  def jwt(token: Token): String = {
    val claims = Token.getJWTClaims(token)
    Token.encodeJWTClaims(claims).fold(f ⇒ "", a ⇒ a)
  }

  implicit val decodeJwtClaims: Decoder[JwtClaims] =
    Decoder.decodeJson.map(json ⇒ JwtClaims.parse(json.jacksonPrint))
  implicit val encodeJwtClaims: Encoder[JwtClaims] = new Encoder[JwtClaims] {
    // yay defensive programming,
    // because I'm not sure if jose4j may return json string, or e.g. json object
    // there is jwt-circe though, so might be valuable to switch
    def apply(a: JwtClaims): Json = parse(a.toJson).getOrElse(Encoder.encodeString(a.toJson))
  }
}
