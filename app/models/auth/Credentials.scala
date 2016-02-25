package models.auth

sealed trait Credentials

final case class SecretCredentials(identifier: String, secret: String) extends Credentials
final case class JWTCredentials(token: String) extends Credentials
final case class OauthCredentials(token: String) extends Credentials
