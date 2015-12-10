package consumer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.stream.ActorMaterializer

import consumer.activity.PhoenixConnectionInfo
import consumer.activity.ActivityProcessor
import consumer.activity.CustomerConnector
import consumer.activity.AdminConnector
import consumer.activity.ActivityConnectionTransformer

import consumer.elastic.ElasticSearchProcessor


/**
 * Program which consumes the activity table changes that bottledwater logs in kakfa
 * and links the activities to trails
 */
object ActivityMain {
  val environmentProperty = "env"
  val defaultEnvironment  = "default"

  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()
  
  implicit lazy final val connectionPoolSettings = 
    ConnectionPoolSettings.create(implicitly[ActorSystem]).copy(
      maxConnections  = 32,
      maxOpenRequests = 32,
      maxRetries      = 0)


  def main(args: Array[String]): Unit = {
    // Load configuration parameters & environment
    val conf = ConfigFactory.load()
    val env = sys.props.getOrElse(environmentProperty, defaultEnvironment)
    println(s"Initializing consumers in $env environment...")

    // Load configuration values
    val avroSchemaRegistryUrl = conf.getString(s"$env.avro.schemaRegistryUrl")
    val elasticSearchUrl      = conf.getString(s"$env.elastic.host")
    val elasticSearchCluster  = conf.getString(s"$env.elastic.cluster")
    val elasticSearchIndex    = conf.getString(s"$env.elastic.index")
    val kafkaBroker           = conf.getString(s"$env.kafka.broker")
    val kafkaGroupId          = conf.getString(s"$env.kafka.groupId")
    val activityTopic         = conf.getString(s"$env.activity.kafka.topic")
    val connectionTopic       = conf.getString(s"$env.activity.connection.kafka.topic")
    val phoenixUri            = conf.getString(s"$env.activity.phoenix.url")
    val phoenixUser           = conf.getString(s"$env.activity.phoenix.user")
    val phoenixPass           = conf.getString(s"$env.activity.phoenix.pass")

    val phoenix = PhoenixConnectionInfo(
      uri = phoenixUri, 
      user = phoenixUser,
      pass = phoenixPass)

    val activityWork = Future {

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

    val trailWork = Future {

      val transformers = Map(connectionTopic →  ActivityConnectionTransformer(phoenix))
      val topics = Seq(connectionTopic)

      // Init processors & consumer
      val esProcessor = new ElasticSearchProcessor(
        uri = elasticSearchUrl, 
        cluster = elasticSearchCluster,
        indexName = elasticSearchIndex, 
        topics = topics,
        jsonTransformers = transformers)

      val avroProcessor = new AvroProcessor(
        schemaRegistryUrl = avroSchemaRegistryUrl, 
        processor = esProcessor)

      val consumer = new MultiTopicConsumer(
        topics = topics, 
        broker = kafkaBroker, 
        groupId = kafkaGroupId,
        processor = avroProcessor)

      // Execture beforeAction
      println("Executing pre-consuming actions...")
      esProcessor.beforeAction()

      // Start consuming & processing
      println(s"Reading from broker $kafkaBroker")
      consumer.readForever()


    }

    Await.ready(activityWork, Duration.Inf)
    Await.ready(trailWork, Duration.Inf)
  }
}


