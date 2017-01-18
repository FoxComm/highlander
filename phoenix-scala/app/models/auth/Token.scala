package models.auth

import java.io.{FileInputStream, InputStream}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

import cats.data.Xor
import failures.AuthFailures._
import failures.{Failures, GeneralFailure}
import models.account.{Account, User}
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.json4s._
import org.json4s.jackson.JsonMethods._
import utils.FoxConfig.{RichConfig, config}
import utils.db._

object Keys {

  case class KeyLoadException(cause: Throwable) extends Exception

  private def loadKeyAsStream(fileName: String): InputStream = {
    config.getOptString("auth.keysLocation") match {
      case Some("jar") ⇒
        getClass.getResourceAsStream(fileName)
      case _ ⇒
        new FileInputStream(fileName)
    }
  }

  def loadPrivateKey: Try[PrivateKey] = Try {
    val fileName = config.getOptString("auth.privateKey").getOrElse("")
    val is       = loadKeyAsStream(fileName)
    val keyBytes = Array.ofDim[Byte](is.available)
    is.read(keyBytes)
    is.close()

    val spec = new PKCS8EncodedKeySpec(keyBytes)
    KeyFactory.getInstance("RSA").generatePrivate(spec)
  }

  def loadPublicKey: Try[PublicKey] =
    Try {
      val fileName = config.getOptString("auth.publicKey").getOrElse("")
      val is       = loadKeyAsStream(fileName)
      val keyBytes = Array.ofDim[Byte](is.available)
      is.read(keyBytes)
      is.close()

      val spec = new X509EncodedKeySpec(keyBytes)
      KeyFactory.getInstance("RSA").generatePublic(spec)
    }.recover {
      case e ⇒ throw KeyLoadException(e)
    }

  private[auth] lazy val authPrivateKey: Failures Xor PrivateKey =
    loadPrivateKey.toOption.toXor(GeneralFailure("Server error: can't load private key").single)
  private[auth] lazy val authPublicKey: Failures Xor PublicKey =
    loadPublicKey.toOption.toXor(GeneralFailure("Server error: can't load public key").single)
}

sealed trait Token extends Product {
  val id: Int
  val name: Option[String]
  val email: Option[String]
  val scope: String
  val roles: Seq[String]
  val claims: Account.Claims
  val ratchet: Int
  def encode: Failures Xor String = Token.encode(this)

  def hasRole(test: String): Boolean = {
    roles.contains(test)
  }

  def hasClaim(test: String, actions: List[String]): Boolean = {
    var matches = false;
    claims.foreach {
      case (k, v) ⇒
        matches = matches || (test.startsWith(k) && actions.equals(actions.intersect(v)))
    }
    matches
  }
}

object Token {
  implicit val formats = DefaultFormats
  val algorithmConstraints = new AlgorithmConstraints(
    AlgorithmConstraints.ConstraintType.WHITELIST,
    config.getString("auth.keyAlgorithm"))

  import collection.JavaConversions.seqAsJavaList

  val tokenTTL = config.getOptInt("auth.tokenTTL").getOrElse(5)

  def getJWTClaims(token: Token): JwtClaims = {
    val claims = new JwtClaims

    //TODO probably put scope here in the future.
    claims.setAudience("user")

    claims.setClaim("id", token.id)
    claims.setClaim("email", token.email)
    claims.setClaim("ratchet", token.ratchet)
    claims.setClaim("scope", token.scope)
    claims.setStringListClaim("roles", token.roles)

    token.name.map { name ⇒
      claims.setClaim("name", name)
      name
    }

    token.email.map { email ⇒
      claims.setClaim("email", email)
      email
    }

    claims.setClaim("claims", token.claims.mapValues(_.asJava).asJava)

    claims.setExpirationTimeMinutesInTheFuture(tokenTTL.toFloat)
    claims.setIssuer("FC")

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
        .setJwsAlgorithmConstraints(algorithmConstraints)
        .setExpectedIssuer("FC")
        .setVerificationKey(publicKey)
        .setExpectedAudience("user")

      Try {
        val consumer  = builder.build()
        val jwtClaims = consumer.processToClaims(rawToken)
        val jValue    = parse(jwtClaims.toJson)
        Extraction.extract[UserToken](jValue)
      } match {
        case Success(token) ⇒ Xor.right(token)
        case Failure(e) ⇒
          System.err.println(e.getMessage)
          Xor.left(AuthFailed(e.getMessage).single)
      }
    }
  }
}

case class UserToken(id: Int,
                     name: Option[String],
                     email: Option[String],
                     roles: Seq[String],
                     scope: String,
                     ratchet: Int,
                     claims: Account.Claims)
    extends Token {

  require(!scope.isEmpty)
  //can't have claims without roles. Either no claims and no roles or claims and roles.
  require((roles.isEmpty && claims.isEmpty) || !(roles.isEmpty || claims.isEmpty))
}

object UserToken {
  def fromUserAccount(user: User, account: Account, claimSet: Account.ClaimSet): UserToken = {
    require(user.accountId == account.id)
    UserToken(id = user.accountId,
              name = user.name,
              email = user.email,
              scope = claimSet.scope,
              roles = claimSet.roles,
              ratchet = account.ratchet,
              claims = claimSet.claims)
  }
}
