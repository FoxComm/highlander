package models.auth

import java.io.FileInputStream
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

import scala.util.{Failure, Success, Try}

import cats.implicits._
import cats.data.Xor
import failures.AuthFailures._
import failures.{Failures, GeneralFailure}
import models.StoreAdmin
import models.customer.Customer
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.{InvalidJwtException, JwtConsumerBuilder}
import org.json4s.jackson.JsonMethods._
import org.json4s.{Extraction, _}
import utils.FoxConfig.{RichConfig, config}
import utils.db._

object Keys {

  case class KeyLoadException(cause: Throwable) extends Exception

  def loadPrivateKey: Try[PrivateKey] = Try {
    val fileName = config.getOptString("auth.privateKey").getOrElse("")
    val is       = new FileInputStream(fileName)
    val keyBytes = Array.ofDim[Byte](is.available)
    is.read(keyBytes)
    is.close()

    val spec = new PKCS8EncodedKeySpec(keyBytes)
    KeyFactory.getInstance("RSA").generatePrivate(spec)
  }

  def loadPublicKey: Try[PublicKey] =
    Try {
      val fileName = config.getOptString("auth.publicKey").getOrElse("")
      val is       = new FileInputStream(fileName)
      val keyBytes = Array.ofDim[Byte](is.available)
      is.read(keyBytes)
      is.close()

      val spec = new X509EncodedKeySpec(keyBytes)
      KeyFactory.getInstance("RSA").generatePublic(spec)
    }.recover {
      case e ⇒ throw KeyLoadException(e)
    }

  private[auth] lazy val authPrivateKey: Failures Xor PrivateKey =
    loadPrivateKey.toOption.toXor(GeneralFailure("Server error: can't load key").single)
  private[auth] lazy val authPublicKey: Failures Xor PublicKey =
    loadPublicKey.toOption.toXor(GeneralFailure("Server error: can't load key").single)
}

sealed trait Token extends Product {
  val id: Int
  val admin: Boolean
  val name: Option[String]
  val email: Option[String]
  val scopes: Seq[String]
  val ratchet: Int
  def encode: Failures Xor String = Token.encode(this)
}

object Token {
  implicit val formats = DefaultFormats
  import collection.JavaConversions.seqAsJavaList

  val tokenTTL = config.getOptInt("auth.tokenTTL").getOrElse(5)

  def getJWTClaims(token: Token): JwtClaims = {
    val claims = new JwtClaims

    claims.setClaim("id", token.id)
    claims.setClaim("email", token.email)
    claims.setClaim("ratchet", token.ratchet)
    claims.setStringListClaim("scopes", token.scopes)

    token.name.map { name ⇒
      claims.setClaim("name", name)
      name
    }

    token.email.map { email ⇒
      claims.setClaim("email", email)
      email
    }

    claims.setExpirationTimeMinutesInTheFuture(tokenTTL.toFloat)
    claims.setIssuer("FC")
    token match {
      case _: AdminToken ⇒ {
        claims.setAudience("admin")
        claims.setClaim("admin", true)
      }
      case _: CustomerToken ⇒ {
        claims.setAudience("customer")
        claims.setClaim("admin", false)
      }
    }

    claims
  }

  def encode(token: Token): Failures Xor String = {
    val claims = Token.getJWTClaims(token)
    encodeJWTClaims(claims)
  }

  def encodeJWTClaims(claims: JwtClaims): Failures Xor String = {
    Keys.authPrivateKey.map { privateKey ⇒
      val jws = new JsonWebSignature
      jws.setPayload(claims.toJson)
      jws.setKey(privateKey)
      jws.setAlgorithmHeaderValue(config.getString("auth.keyAlgorithm"))
      jws.getCompactSerialization
    }
  }

  def fromString(rawToken: String, kind: Identity.IdentityKind): Failures Xor Token = {
    Keys.authPublicKey.flatMap { publicKey ⇒
      val builder = new JwtConsumerBuilder()
        .setRequireExpirationTime()
        .setAllowedClockSkewInSeconds(30)
        .setExpectedIssuer("FC")
        .setVerificationKey(publicKey)

      Try {
        kind match {
          case Identity.Customer ⇒ builder.setExpectedAudience("customer")
          case Identity.Admin    ⇒ builder.setExpectedAudience("admin")
          case _                 ⇒ throw new RuntimeException("unknown kind of identity")
        }

        val consumer  = builder.build()
        val jwtClaims = consumer.processToClaims(rawToken)
        val jValue    = parse(jwtClaims.toJson)
        jValue \ "admin" match {
          case JBool(isAdmin) ⇒
            if (isAdmin)
              Extraction.extract[AdminToken](jValue)
            else
              Extraction.extract[CustomerToken](jValue)
          case _ ⇒ throw new InvalidJwtException(s"missing claim: admin")
        }
      } match {
        case Success(token) ⇒ Xor.right(token)
        case Failure(e) ⇒
          System.err.println(e.getMessage)
          Xor.left(AuthFailed(e.getMessage).single)
      }
    }
  }
}

case class AdminToken(id: Int,
                      admin: Boolean = true,
                      name: Option[String],
                      email: Option[String],
                      scopes: Seq[String],
                      department: Option[String] = None,
                      ratchet: Int)
    extends Token

object AdminToken {
  def fromAdmin(admin: StoreAdmin): AdminToken = {
    AdminToken(id = admin.id,
               name = admin.name.some,
               email = admin.email.some,
               scopes = Array("admin"),
               department = admin.department,
               ratchet = admin.ratchet)
  }
}

case class CustomerToken(id: Int,
                         admin: Boolean = false,
                         name: Option[String],
                         email: Option[String],
                         scopes: Seq[String],
                         ratchet: Int)
    extends Token

object CustomerToken {
  def fromCustomer(customer: Customer): CustomerToken =
    CustomerToken(id = customer.id,
                  name = customer.name,
                  email = customer.email,
                  scopes = Array[String](),
                  ratchet = customer.ratchet)
}
