package consumer

import scala.collection.JavaConversions._
import com.typesafe.config.ConfigFactory
import consumer.utils.PhoenixConnectionInfo

final case class MainConfig(activityTopic: String,
                            avroSchemaRegistryUrl: String,
                            elasticSearchCluster: String,
                            elasticSearchIndex: String,
                            elasticSearchUrl: String,
                            kafkaBroker: String,
                            kafkaGroupId: String,
                            indexTopics: MainConfig.IndexTopicMap,
                            phoenixPass: String,
                            phoenixUri: String,
                            phoenixUser: String,
                            phoenixOrg: String,
                            maxConnections: Int,
                            startFromBeginning: Boolean,
                            doSetup: Boolean) {

  def connectionInfo(): PhoenixConnectionInfo =
    PhoenixConnectionInfo(phoenixUri, phoenixUser, phoenixPass, phoenixOrg)
}

object MainConfig {
  type IndexTopicMap = Map[String, Seq[String]];
  val environmentProperty = "env"
  val defaultEnvironment  = "default"

  def loadFromConfig(): MainConfig = {
    val conf = ConfigFactory.load()
    val env  = sys.props.getOrElse(environmentProperty, defaultEnvironment)
    Console.out.println(s"""Loading config for "$env" environment...""")

    val topicConf = conf.getConfig("kafka.indices");
    val topics = topicConf.entrySet.foldLeft(Map[String, Seq[String]]()) {
      case (m, entry) ⇒ {
          m + (entry.getKey → topicConf.getStringList(entry.getKey).toSeq)
        }
    }

    MainConfig(
        activityTopic = conf.getString(s"$env.activity.kafka.topic"),
        avroSchemaRegistryUrl = conf.getString(s"$env.avro.schemaRegistryUrl"),
        elasticSearchCluster = conf.getString(s"$env.elastic.cluster"),
        elasticSearchIndex = conf.getString(s"$env.elastic.index"),
        elasticSearchUrl = conf.getString(s"$env.elastic.host"),
        kafkaBroker = conf.getString(s"$env.kafka.broker"),
        kafkaGroupId = conf.getString(s"$env.kafka.groupId"),
        indexTopics = topics,
        phoenixPass = conf.getString(s"$env.activity.phoenix.pass"),
        phoenixUri = conf.getString(s"$env.activity.phoenix.url"),
        phoenixUser = conf.getString(s"$env.activity.phoenix.user"),
        phoenixOrg = conf.getString(s"$env.activity.phoenix.org"),
        maxConnections = conf.getInt(s"$env.max.connections"),
        startFromBeginning = conf.getBoolean(s"$env.consume.restart"),
        doSetup = conf.getBoolean(s"$env.elastic.setup")
    )
  }
}
