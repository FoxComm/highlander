package foxcomm.search

import com.typesafe.config.ConfigFactory
import pureconfig._

final case class AppConfig(http: AppConfig.Http, elasticsearch: AppConfig.ElasticSearch)

object AppConfig {
  final case class Http(interface: String, port: Int)

  final case class ElasticSearch(host: String, cluster: String)

  def load(): AppConfig = {
    val config =
      ConfigFactory.systemProperties.withFallback(ConfigFactory.load())
    loadConfigOrThrow[AppConfig](config, "app")
  }
}
