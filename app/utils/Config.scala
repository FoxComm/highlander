package utils

import cats.Show
import cats.implicits._
import com.typesafe.config.{Config ⇒ TypesafeConfig, ConfigFactory}

object Config {
  sealed trait Environment
  case object Test extends Environment
  case object Development extends Environment
  case object Staging extends Environment
  case object Production extends Environment

  implicit val environmentShow = new Show[Environment] {
    def show(e: Environment) = friendlyClassName(e).toLowerCase
  }

  def environment: Environment = sys.env.get("PHOENIX_ENV") match {
    case Some("test")         ⇒ Test
    case Some("staging")      ⇒ Staging
    case Some("production")   ⇒ Production
    case _                    ⇒ Development
  }

  def loadWithEnv(env: Environment = environment, cfg: TypesafeConfig = ConfigFactory.load): TypesafeConfig = {
    val envConfig = cfg.getConfig(s"env." ++ env.show)
    ConfigFactory.systemProperties.withFallback(envConfig.withFallback(cfg))
  }
}
