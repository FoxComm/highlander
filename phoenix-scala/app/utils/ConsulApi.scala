package utils

import com.orbitz.consul.Consul
import utils.FoxConfig._

class ConsulApi(consulUrl: String) {
  val consul   = Consul.builder().withUrl(consulUrl).build()
  val kvClient = consul.keyValueClient()
}

object ConsulApi {
  val client = new ConsulApi(config.apis.consul.url)

  def get(key: String): Option[String] = {
    val value = client.kvClient.getValueAsString(key)
    if (value.isPresent)
      Option(value.get)
    else None
  }

  def set(key: String, value: String): Boolean = {
    client.kvClient.putValue(key, value)
  }
}
