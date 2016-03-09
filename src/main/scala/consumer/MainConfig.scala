package consumer

import scala.collection.JavaConversions._
import com.typesafe.config.ConfigFactory
import consumer.utils.PhoenixConnectionInfo

final case class MainConfig(activityTopic: String, avroSchemaRegistryUrl: String, connectionTopic: String,
  elasticSearchCluster: String, elasticSearchIndex: String, elasticSearchUrl: String, kafkaBroker: String,
  kafkaGroupId: String, kafkaTopics: Seq[String], phoenixPass: String, phoenixUri: String, phoenixUser: String,
  maxConnections: Int, startFromBeginning: Boolean, doSetup: Boolean) {

  def topicsPlusActivity(): Seq[String] = kafkaTopics :+ connectionTopic

  def connectionInfo(): PhoenixConnectionInfo = PhoenixConnectionInfo(phoenixUri, phoenixUser, phoenixPass)
}

object MainConfig {
  val environmentProperty = "env"
  val defaultEnvironment  = "default"

  def loadFromConfig(): MainConfig = {
    val conf = ConfigFactory.load()
    val env = sys.props.getOrElse(environmentProperty, defaultEnvironment)
    Console.out.println(s"Initializing Using $env Environment...")

    MainConfig(
      activityTopic         = conf.getString(s"$env.activity.kafka.topic"),
      avroSchemaRegistryUrl = conf.getString(s"$env.avro.schemaRegistryUrl"),
      connectionTopic       = conf.getString(s"$env.activity.connection.kafka.topic"),
      elasticSearchCluster  = conf.getString(s"$env.elastic.cluster"),
      elasticSearchIndex    = conf.getString(s"$env.elastic.index"),
      elasticSearchUrl      = conf.getString(s"$env.elastic.host"),
      kafkaBroker           = conf.getString(s"$env.kafka.broker"),
      kafkaGroupId          = conf.getString(s"$env.kafka.groupId"),
      kafkaTopics           = conf.getStringList(s"kafka.topics").toIndexedSeq.toSeq,
      phoenixPass           = conf.getString(s"$env.activity.phoenix.pass"),
      phoenixUri            = conf.getString(s"$env.activity.phoenix.url"),
      phoenixUser           = conf.getString(s"$env.activity.phoenix.user"),
      maxConnections        = conf.getInt(s"$env.max.connections"),
      startFromBeginning    = conf.getBoolean(s"$env.consume.restart"),
      doSetup               = conf.getBoolean(s"$env.elastic.setup")
    )
  }
}
