package foxcomm.agni

import cats.data.NonEmptyList
import com.typesafe.config.ConfigFactory
import java.net.InetSocketAddress
import pureconfig._
import scala.util.Try

final case class AppConfig(http: AppConfig.Http, elasticsearch: AppConfig.ElasticSearch)

@SuppressWarnings(Array("org.wartremover.warts.Equals"))
object AppConfig {
  implicit val readHostConfig: ConfigReader[NonEmptyList[InetSocketAddress]] =
    ConfigReader.fromNonEmptyStringTry(s ⇒
      Try {
        val withoutPrefix = s.stripPrefix("elasticsearch://")
        val hosts = withoutPrefix.split(',').map { host ⇒
          val parts = host.split(':')
          require(parts.length == 2,
                  "ElasticSearch uri must be in format elasticsearch://host:port,host:port,...")
          new InetSocketAddress(parts(0), parts(1).toInt)
        }
        require(hosts.length >= 1, "At least single ElasticSearch host should be specified")
        NonEmptyList.fromListUnsafe(hosts.toList)
    })

  final case class Http(interface: String, port: Int)

  final case class ElasticSearch(host: NonEmptyList[InetSocketAddress], cluster: String)

  def load(): AppConfig = {
    val config =
      ConfigFactory.systemProperties.withFallback(ConfigFactory.load())
    loadConfigOrThrow[AppConfig](config, "app")
  }
}
