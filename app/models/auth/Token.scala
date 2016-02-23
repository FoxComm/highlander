package models.auth

import java.io.FileInputStream
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

import scala.util.{Failure, Success, Try}

import cats.data.Xor
import models.StoreAdmin
import models.customer.Customer
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.{InvalidJwtException, JwtConsumerBuilder}
import org.json4s.jackson.JsonMethods._
import org.json4s.{Extraction, _}
import services.{Failures, LoginFailed}
import utils.Config.config
import utils.caseClassToMap


object Keys {

  def loadPrivateKey: PrivateKey = {
    val fileName = config.getString("auth.privateKey")
    val is = new FileInputStream(fileName)
    val keyBytes = Array.ofDim[Byte](is.available)
    is.read(keyBytes)
    is.close()

    val spec = new PKCS8EncodedKeySpec(keyBytes)
    KeyFactory.getInstance("RSA").generatePrivate(spec)
  }

  def loadPublicKey: PublicKey = {
    val fileName = config.getString("auth.publicKey")
    val is = new FileInputStream(fileName)
    val keyBytes = Array.ofDim[Byte](is.available)
    is.read(keyBytes)
    is.close()

    val spec = new X509EncodedKeySpec(keyBytes)
    KeyFactory.getInstance("RSA").generatePublic(spec)
  }

  val authPrivateKey = loadPrivateKey
  val authPublicKey = loadPublicKey
}


sealed trait Token extends Product {
  val id: Int
  val admin: Boolean
  val name: Option[String]
  val email: String
  val scopes: Seq[String]

  def encode: String = {
    Token.encode(this)
  }
}

object Token {
  implicit val formats = DefaultFormats
  import collection.JavaConversions.seqAsJavaList

  private def getJWTClaims(token: Token): JwtClaims = {
    val claims = new JwtClaims

    for ((field, value) ← caseClassToMap(token)) {
      value match {
        case value: Seq[_] ⇒ claims.setStringListClaim(field, value.asInstanceOf[Seq[String]].toList)
        case None ⇒ claims
        case _ ⇒ claims.setClaim(field, value)
      }
    }

    claims.setExpirationTimeMinutesInTheFuture(150)
    claims.setIssuer("FC")
    claims
  }

  def encode(token: Token): String = {
    val claims = Token.getJWTClaims(token)

    val jws = new JsonWebSignature
    jws.setPayload(claims.toJson)
    jws.setKey(Keys.authPrivateKey)
    jws.setAlgorithmHeaderValue(config.getString("auth.keyAlgorithm"))
    jws.getCompactSerialization
  }


  def fromString(rawToken: String): Failures Xor Token = {
    val builder = new JwtConsumerBuilder()
      .setRequireExpirationTime()
      .setAllowedClockSkewInSeconds(30)
      .setExpectedIssuer("FC")
      .setVerificationKey(Keys.authPublicKey)
      .build()

    Try {
      val jwtClaims = builder.processToClaims(rawToken)
      val jValue = parse(jwtClaims.toJson)
      (jValue \ "admin") match {
        case JBool(isAdmin) ⇒ if (isAdmin)
          Extraction.extract[AdminToken](jValue)
        else
          Extraction.extract[CustomerToken](jValue)
        case _ ⇒ throw new InvalidJwtException("missing claim")
      }
    } match {
      case Success(token) ⇒ Xor.right(token)
      // TODO: handle failures
      case Failure(e) ⇒ {
        System.err.println(e.getMessage)
        Xor.left(LoginFailed.single)
      }
    }
  }

}

final case class AdminToken(id: Int,
  admin: Boolean = true,
  name: Option[String],
  email: String,
  scopes: Seq[String],
  department: Option[String] = None
) extends Token

object AdminToken {
  def fromAdmin(admin: StoreAdmin): AdminToken = {
    AdminToken(id = admin.id, name = Some(admin.name), email = admin.email,
      scopes = Array("admin"),
      department = admin.department)
  }
}


final case class CustomerToken(id: Int,
  admin: Boolean = false,
  name: Option[String],
  email: String,
  scopes: Seq[String]
  ) extends Token

object CustomerToken {
  def fromCustomer(customer: Customer): CustomerToken = {
    CustomerToken(id = customer.id, name = customer.name, email = customer.email,
      scopes = Array[String]())
  }
}