package utils


import cats.Show
import cats.implicits._
import com.typesafe.config.{Config ⇒ TypesafeConfig, ConfigFactory}


object Config {

  implicit class RichConfig(val underlying: TypesafeConfig) extends AnyVal {
    private[this] def getOptionalSetting[A](finder: (String ⇒ A))(path: String): Option[A] = {
      if (underlying.hasPath(path)) {
        Some(finder(path))
      } else {
        None
      }
    }

    def getOptBool = getOptionalSetting[Boolean](underlying.getBoolean)(_)
    def getOptString = getOptionalSetting[String](underlying.getString)(_)
  }

  sealed trait Environment
  case object Test extends Environment
  case object Development extends Environment
  case object Staging extends Environment
  case object Production extends Environment

  implicit val environmentShow: Show[Environment] = new Show[Environment] {
    def show(e: Environment) = friendlyClassName(e).toLowerCase
  }

  def environment: Environment = sys.env.get("PHOENIX_ENV") match {
    case Some("test")         ⇒ Test
    case Some("staging")      ⇒ Staging
    case Some("production")   ⇒ Production
    case _                    ⇒ Development
  }

  def loadWithEnv(cfg: TypesafeConfig = ConfigFactory.load)(implicit env: Environment = environment): TypesafeConfig = {
    val envConfig = cfg.getConfig(s"env." ++ env.show)
    val config = ConfigFactory.systemProperties.withFallback(envConfig.withFallback(cfg))

    ensureRequiredSettingsIsSet(config)
    config
  }

  lazy val config = loadWithEnv()

  private def ensureRequiredSettingsIsSet(config: TypesafeConfig) = {
    for {
      stringKey ← Seq("auth.privateKey", "auth.publicKey", "auth.keyAlgorithm")
    } yield config.getString(stringKey)
  }
}
