package consumer

import java.util.concurrent.Executors

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.stream.ActorMaterializer

import consumer.activity.ActivityConnectionTransformer
import consumer.activity.ActivityProcessor
import consumer.activity.AdminConnector
import consumer.activity.CustomerConnector
import consumer.elastic.AvroTransformers
import consumer.elastic.ElasticSearchProcessor
import consumer.utils.PhoenixConnectionInfo

/**
 * Program which consumes several bottledwater topics and indexes them in Elastic Search
 */


/**
 * I apologize for nothing!
                                                                                                    
                                                                     ...--------:..`                
                                                                 `..-...........:...-.`             
                                                               `.-...--.........:......-`           
                                                              ...-..-..:-.---..-.........-          
                                                             ....`  -..-`   `.-...........:         
                                                             -..    -`.-      `-........----        
                                                            `-`.  ``-...       .--------...-`       
                                        `````                :`.`..-...         :...........-       
              :``..`/```      -     `..-.......`             ````--..           -...........-       
          .---:..----...--.`  -  ```.`..........-``          ` `````           -............-       
        .-:--.......-.......--: ``````...............`       ``````           .:---........`-       
        :-...........:.......:..````.......---..-....:-       ````           .-.-..--......-`       
       /------.-.....-.......-...`...-.``` ``-`-     .-.      ````         `-.........--...-        
      /-......----..-:-..----...:`.....`   ``....`  ``:`       `         .--............-.-`        
      /...........:::`````..---.:`.......`........`....-`            ``.------..........--`         
     -:...........:..---.---.---:`......................-.`````````.---......--.........-`          
     -:.........--/:.          .-.................`  -...-...--:-.--...........-........`           
     ./--.--..---..s:           .-.........```   .   `---.......--..-...........-....-`             
     .+-....--.....:s           .-........   ``   .   `-..........:.-...........:....`              
      ./...........-o.          .:....`- `    .   `.   `-..........:.-.........:.-.``.```           
      .-/-........-.+.          ..-...`-  .    `   ..`.`---.........--....-.---:.`..`.````.``       
         :--......--/.           .-....-.````..-...........--..-..--:::-.`..--...``.````````-       
         .--/--.----/.            -......----.....---......-------....```````.``.```.`.``..---`     
             /::---::.            .-..........----..........-....````` ````.```` ````.```````..-.`  
                   `.:.           .-........-...............--...-.```````.``````. .. ```` ` .....  
                    `o.            .-....-..................--:--.-.```````.`.```````.`.```....-.`  
                    `::            ..-.......................------..`````---..--..``.---....-:..`  
                    `:-.            .--........................-:....````...---.....----.---.-...-..
                    `- :            .:..........................--....```......-----..-...-------..`
                     - :            -............................:..``......................-.......
                     - -.           -................................-...................--........`
                     - -.          -`.........................``......-..-----.--------.............
                     -  /          -`........................`...........`.....-.......``...........
                   ``-  :..........-``````````````````..``````````````..`````````````.``````````````
**/

object Main {
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
    val activityTopic         = conf.getString(s"$env.activity.kafka.topic")
    val avroSchemaRegistryUrl = conf.getString(s"$env.avro.schemaRegistryUrl")
    val connectionTopic       = conf.getString(s"$env.activity.connection.kafka.topic")
    val elasticSearchCluster  = conf.getString(s"$env.elastic.cluster")
    val elasticSearchIndex    = conf.getString(s"$env.elastic.index")
    val elasticSearchUrl      = conf.getString(s"$env.elastic.host")
    val kafkaBroker           = conf.getString(s"$env.kafka.broker")
    val kafkaGroupId          = conf.getString(s"$env.kafka.groupId")
    val kafkaTopics           = conf.getStringList(s"kafka.topics").toIndexedSeq.toSeq
    val phoenixPass           = conf.getString(s"$env.activity.phoenix.pass")
    val phoenixUri            = conf.getString(s"$env.activity.phoenix.url")
    val phoenixUser           = conf.getString(s"$env.activity.phoenix.user")
    val threadPoolCount       = conf.getInt(s"$env.thread.pool")

    val threadPool = java.util.concurrent.Executors.newFixedThreadPool(threadPoolCount)
    implicit val ec = ExecutionContext.fromExecutor(threadPool)

    Console.err.println(s"ES: ${elasticSearchUrl}")
    Console.err.println(s"Kafka: ${kafkaBroker}")
    Console.err.println(s"Schema Registry: ${avroSchemaRegistryUrl}")
    Console.err.println(s"Phoenix: ${phoenixUri}")

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
        groupId = s"${kafkaGroupId}_activity",
        processor = avroProcessor)

      // Start consuming & processing
      println(s"Reading from broker $kafkaBroker")

      consumer.readForever()
    }

    val trailWork = Future {
      val transformers = Map(
        "regions" → AvroTransformers.Region(),
        "countries"  → AvroTransformers.Country(),
        "customers_search_view" →  AvroTransformers.CustomerSearchView(),
        "orders_search_view" →  AvroTransformers.OrderSearchView(),
        connectionTopic → ActivityConnectionTransformer(phoenix))

      // Init processors & consumer
      val esProcessor = new ElasticSearchProcessor(
        uri = elasticSearchUrl, 
        cluster = elasticSearchCluster,
        indexName = elasticSearchIndex, 
        topics = kafkaTopics,
        jsonTransformers = transformers)

      val avroProcessor = new AvroProcessor(
        schemaRegistryUrl = avroSchemaRegistryUrl, 
        processor = esProcessor)

      val consumer = new MultiTopicConsumer(
        topics = kafkaTopics, 
        broker = kafkaBroker, 
        groupId = s"${kafkaGroupId}_trail",
        processor = avroProcessor)

      // Execture beforeAction
      println("Executing pre-consuming actions...")
      esProcessor.beforeAction()

      // Start consuming & processing
      println(s"Reading from broker $kafkaBroker")
      consumer.readForever()
    }

    activityWork onFailure { 
      case t ⇒ Console.err.println(s"Error occurred consuming activities: ${t.getMessage}")
    }

    trailWork onFailure { 
      case t ⇒ Console.err.println(s"Error occurred indexing to ES: ${t}")
    }
    //These threads will actually never be ready. 
    //This is a hedonist bot.
    Await.ready(activityWork, Duration.Inf)
    Await.ready(trailWork, Duration.Inf)
  }
}
