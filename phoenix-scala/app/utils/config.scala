package utils

import cats.Show
import cats.implicits._
import com.pellucid.sealerate
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig._
import scala.reflect._
import scala.util.{Failure, Success, Try}
import shapeless._
import utils.FoxConfig._

sealed trait Environment {
  def isProd: Boolean = false
}
object Environment {
  case object Test        extends Environment
  case object Development extends Environment
  case object Staging     extends Environment
  case object Production extends Environment {
    override def isProd: Boolean = true
  }

  implicit val environmentShow: Show[Environment] = Show.show(friendlyClassName(_).toLowerCase)

  implicit lazy val default: Environment =
    sys.props.get("phoenix.env").orElse(sys.env.get("PHOENIX_ENV")) match {
      case Some("test")       ⇒ Test
      case Some("staging")    ⇒ Staging
      case Some("production") ⇒ Production
      case _                  ⇒ Development
    }
}

case class FoxConfig(apis: Apis,
                     app: App,
                     auth: Auth,
                     db: DB,
                     http: Http,
                     taxRules: TaxRules,
                     users: Users)

object FoxConfig {
  implicit val booleanConfigConvert: ConfigConvert[Boolean] = ConfigConvert.fromNonEmptyString {
    case "true" | "yes" | "on"  ⇒ Success(true)
    case "false" | "no" | "off" ⇒ Success(false)
    case s                      ⇒ Failure(new IllegalArgumentException(s"Could not interpret '$s' as a boolean"))
  }

  implicit def configConvertADT[T: ADT: ClassTag]: ConfigConvert[T] =
    ConfigConvert.nonEmptyStringConvert(
        s ⇒
          ADT[T].read(s).map(Success(_)).getOrElse {
            val err =
              s"Could not interpret '$s' as a member of ${classTag[T].runtimeClass.getSimpleName}."
            Failure(new IllegalArgumentException(err))
        },
        ADT[T].show)

  case class App(defaultContextId: Int,
                 overrideHashPasswordAlgorithm: Option[HashPasswords.HashAlgorithm])

  // auth
  case class Auth(cookie: Cookie,
                  publicKey: String,
                  privateKey: String,
                  method: AuthMethod,
                  keyAlgorithm: String,
                  keysLocation: KeysLocation = KeysLocation.File,
                  tokenTTL: Int = 5)

  sealed trait AuthMethod
  implicit object AuthMethod extends ADT[AuthMethod] {
    case object Basic extends AuthMethod
    case object Jwt   extends AuthMethod

    def types = sealerate.values[AuthMethod]
  }

  sealed trait KeysLocation
  implicit object KeysLocation extends ADT[KeysLocation] {
    case object Jar  extends KeysLocation
    case object File extends KeysLocation

    def types = sealerate.values[KeysLocation]
  }

  case class Cookie(domain: Option[String], ttl: Option[Long], secure: Boolean = true)

  // apis
  case class Apis(aws: AWS, elasticsearch: ESConfig, middlewarehouse: MWH, stripe: Stripe)
  case class AWS(accessKey: String, secretKey: String, s3Bucket: String, s3Region: String)
  case class ESConfig(host: String, cluster: String, index: String)
  case class MWH(url: String)
  case class Stripe(key: String)

  // db
  case class DB(url: String)

  // http
  case class Http(interface: String, port: Int)

  // tax
  case class TaxRules(regionId: Option[Int], rate: Option[Double])

  // users
  case class Users(admin: User, customer: User)
  case class User(role: String, org: String, scopeId: Int, oauth: Oauth)
  case class Oauth(google: GoogleOauth)
  case class GoogleOauth(clientId: String,
                         clientSecret: String,
                         redirectUri: String,
                         hostedDomain: Option[String])

  private def loadBareConfigWithEnv(cfg: Config = ConfigFactory.load)(
      implicit env: Environment): Config = {
    val envConfig = ConfigFactory.load(env.show)
    ConfigFactory.systemProperties.withFallback(envConfig.withFallback(cfg))
  }

  val app: Lens[FoxConfig, App] = lens[FoxConfig].app

  val auth: Lens[FoxConfig, Auth] = lens[FoxConfig].auth
  val cookie: Lens[Auth, Cookie]  = lens[Auth].cookie

  val apis: Lens[FoxConfig, Apis] = lens[FoxConfig].apis
  val aws: Lens[Apis, AWS]        = lens[Apis].aws
  val mwh: Lens[Apis, MWH]        = lens[Apis].middlewarehouse
  val stripe: Lens[Apis, Stripe]  = lens[Apis].stripe

  val db: Lens[FoxConfig, DB] = lens[FoxConfig].db

  val http: Lens[FoxConfig, Http] = lens[FoxConfig].http

  val taxRules: Lens[FoxConfig, TaxRules] = lens[FoxConfig].taxRules

  val users: Lens[FoxConfig, Users]        = lens[FoxConfig].users
  val customer: Lens[Users, User]          = lens[Users].customer
  val admin: Lens[Users, User]             = lens[Users].admin
  val googleOauth: Lens[User, GoogleOauth] = lens[User].oauth.google

  // impure, but throwing an exception is exactly what we want here
  val (config, unsafe) = (for {
    underlying ← Try(loadBareConfigWithEnv())
    config     ← loadConfig[FoxConfig](underlying)
  } yield (config, underlying)).get
}
