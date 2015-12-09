package consumer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import com.typesafe.config._

/**
 * Program which consumes several bottledwater topics and indexes them in Elastic Search
 */
object EsMain {
  val environmentProperty = "env"
  val defaultEnvironment  = "default"

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
    val kafkaTopics           = conf.getStringList(s"kafka.topics").toIndexedSeq.toSeq

    // Init processors & consumer
    val esProcessor = new ElasticSearchProcessor(
      uri = elasticSearchUrl, 
      cluster = elasticSearchCluster,
      indexName = elasticSearchIndex, 
      topics = kafkaTopics)

    val avroProcessor = new AvroProcessor(
      schemaRegistryUrl = avroSchemaRegistryUrl, 
      processor = esProcessor)

    val consumer = new MultiTopicConsumer(
      topics = kafkaTopics, 
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
}


