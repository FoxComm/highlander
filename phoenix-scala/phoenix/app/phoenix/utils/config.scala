package phoenix.utils

import cats.Show
import cats.implicits._
import com.pellucid.sealerate
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import core.utils.friendlyClassName
import pureconfig._
import shapeless._
import scala.reflect._
import scala.util.{Failure, Success, Try}

import phoenix.libs.oauth.{FacebookOauthOptions, GoogleOauthOptions, OauthClientOptions}

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

case class FoxConfig(apis: FoxConfig.Apis,
                     app: FoxConfig.App,
                     auth: FoxConfig.Auth,
                     db: FoxConfig.DB,
                     http: FoxConfig.Http,
                     taxRules: FoxConfig.TaxRules,
                     users: FoxConfig.Users)

object FoxConfig extends StrictLogging {
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
      ADT[T].show
    )

  case class App(defaultContextId: Int, overrideHashPasswordAlgorithm: Option[HashPasswords.HashAlgorithm])

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
  case class Apis(aws: AWS, elasticsearch: ESConfig, middlewarehouse: MWH, stripe: Stripe, kafka: Kafka)
  case class AWS(accessKey: String, secretKey: String, s3Bucket: String, s3Region: String)
  case class ESConfig(host: String, cluster: String, index: String)
  case class MWH(url: String)
  case class Stripe(key: String)
  case class Kafka(schemaRegistryURL: String,
                   bootStrapServersConfig: String,
                   producerTimeout: String,
                   keySerializer: String,
                   valueSerializer: String)

  // db
  case class DB(url: String)

  // http
  case class Http(interface: String, port: Int)

  // tax
  case class TaxRules(regionId: Option[Int], rate: Option[Double])

  // users
  case class Users(admin: User, customer: User)
  case class User(role: String, org: String, scopeId: Int, oauth: OauthProviders)

  trait SupportedOauthProviders[T] {
    val google: T
    val facebook: T
  }

  sealed trait OauthProviderName
  implicit object OauthProviderName extends ADT[OauthProviderName] {
    case object Google   extends OauthProviderName
    case object Facebook extends OauthProviderName

    def types = sealerate.values[OauthProviderName]
  }

  case class OauthProviders(google: GoogleOauthOptions, facebook: FacebookOauthOptions)
      extends SupportedOauthProviders[OauthClientOptions]

  private def loadBareConfigWithEnv()(implicit env: Environment): Config = {
    logger.info(s"Loading configuration using ${env.show} environment")
    val envConfig = ConfigFactory.load(env.show)
    ConfigFactory.systemProperties.withFallback(envConfig.withFallback(ConfigFactory.load()))
  }

  /*_*/ // <- disable IDEA linting for the following fragment
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

  val users: Lens[FoxConfig, Users]               = lens[FoxConfig].users
  val customer: Lens[Users, User]                 = lens[Users].customer
  val admin: Lens[Users, User]                    = lens[Users].admin
  val googleOauth: Lens[User, GoogleOauthOptions] = lens[User].oauth.google
  /*_*/

  def loadConfigWithEnv()(implicit env: Environment): Try[(FoxConfig, Config)] =
    for {
      underlying ← Try(loadBareConfigWithEnv())
      config     ← loadConfig[FoxConfig](underlying)
    } yield (config, underlying)

  // impure, but throwing an exception is exactly what we want here
  val (config, unsafe) = loadConfigWithEnv().get
}
