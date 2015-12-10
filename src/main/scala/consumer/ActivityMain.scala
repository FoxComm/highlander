package consumer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import com.typesafe.config._

import consumer.activity.PhoenixConnectionInfo
import consumer.activity.ActivityProcessor
import consumer.activity.CustomerConnector
import consumer.activity.AdminConnector

/**
 * Program which consumes the activity table changes that bottledwater logs in kakfa
 * and links the activities to trails
 */
object ActivityMain {
  val environmentProperty = "env"
  val defaultEnvironment  = "default"

  def main(args: Array[String]): Unit = {
    // Load configuration parameters & environment
    val conf = ConfigFactory.load()
    val env = sys.props.getOrElse(environmentProperty, defaultEnvironment)
    println(s"Initializing consumers in $env environment...")

    // Load configuration values
    val avroSchemaRegistryUrl = conf.getString(s"$env.avro.schemaRegistryUrl")
    val kafkaBroker           = conf.getString(s"$env.kafka.broker")
    val kafkaGroupId          = conf.getString(s"$env.kafka.groupId")
    val activityTopic         = conf.getString(s"$env.activity.kafka.topic")
    val phoenixUri            = conf.getString(s"$env.activity.kafka.phoenix.url")
    val phoenixUser           = conf.getString(s"$env.activity.kafka.phoenix.user")
    val phoenixPass           = conf.getString(s"$env.activity.kafka.phoenix.pass")

    val phoenix = PhoenixConnectionInfo(
      uri = phoenixUri, 
      user = phoenixUser,
      pass = phoenixPass)

    val activityConnectors = Seq(CustomerConnector(), AdminConnector())
    val activityProcessor = new ActivityProcessor(phoenix, activityConnectors)

    val avroProcessor = new AvroProcessor(
      schemaRegistryUrl = avroSchemaRegistryUrl, 
      processor = activityProcessor)

    val consumer = new MultiTopicConsumer(
      topics = Seq(activityTopic), 
      broker = kafkaBroker, 
      groupId = kafkaGroupId,
      processor = avroProcessor)

    // Start consuming & processing
    println(s"Reading from broker $kafkaBroker")
    consumer.readForever()
  }
}


