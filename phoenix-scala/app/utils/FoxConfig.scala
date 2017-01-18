package utils

import cats.Show
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import scala.concurrent.duration._

object FoxConfig {

  implicit class RichConfig(val underlying: Config) extends AnyVal {
    private[this] def getOptionalSetting[A](finder: (String ⇒ A))(path: String): Option[A] = {
      if (underlying.hasPath(path)) {
        Some(finder(path))
      } else {
        None
      }
    }

    def getOptBool   = getOptionalSetting[Boolean](underlying.getBoolean)(_)
    def getOptString = getOptionalSetting[String](underlying.getString)(_)
    def getOptInt    = getOptionalSetting[Int](underlying.getInt)(_)
    def getOptLong   = getOptionalSetting[Long](underlying.getLong)(_)
    def getOptDouble = getOptionalSetting[Double](underlying.getDouble)(_)
    def getOptDuration =
      getOptionalSetting[FiniteDuration](underlying.getDuration(_).toMillis.millis)(_)
  }

  sealed trait Environment
  case object Test        extends Environment
  case object Development extends Environment
  case object Staging     extends Environment
  case object Production  extends Environment

  implicit val environmentShow: Show[Environment] = new Show[Environment] {
    def show(e: Environment) = friendlyClassName(e).toLowerCase
  }

  def environment: Environment =
    sys.props.get("phoenix.env").orElse(sys.env.get("PHOENIX_ENV")) match {
      case Some("test")       ⇒ Test
      case Some("staging")    ⇒ Staging
      case Some("production") ⇒ Production
      case _                  ⇒ Development
    }

  def loadWithEnv(cfg: Config = ConfigFactory.load)(
      implicit env: Environment = environment): Config = {
    val envConfig = cfg.getConfig("env." ++ env.show)
    ConfigFactory.systemProperties.withFallback(envConfig.withFallback(cfg))
  }

  lazy val config = loadWithEnv()
}
