package consumer

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.stream.ActorMaterializer

import consumer.activity.{OrderConnector, ActivityConnectionTransformer, ActivityProcessor, AdminConnector,
CustomerConnector, GiftCardConnector, StoreCreditConnector}
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

final case class MainConfig(
    activityTopic          : String,          
    avroSchemaRegistryUrl  : String, 
    connectionTopic        : String,      
    elasticSearchCluster   : String, 
    elasticSearchIndex     : String, 
    elasticSearchUrl       : String, 
    kafkaBroker            : String, 
    kafkaGroupId           : String, 
    kafkaTopics            : Seq[String], 
    phoenixPass            : String, 
    phoenixUri             : String, 
    phoenixUser            : String, 
    threadPoolCount        : Int,
    startFromBeginning : Boolean)

object MainConfig {
  val environmentProperty = "env"
  val defaultEnvironment  = "default"

  def loadFromConfig : MainConfig = {

    val conf = ConfigFactory.load()
    val env = sys.props.getOrElse(environmentProperty, defaultEnvironment)
    println(s"Initializing consumers in $env environment...")

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
      threadPoolCount       = conf.getInt(s"$env.thread.pool"),
      startFromBeginning    = conf.getBoolean(s"$env.consume.restart"))
  }
}

object Main {

  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()

  implicit lazy final val connectionPoolSettings =
    ConnectionPoolSettings.create(implicitly[ActorSystem]).copy(
      maxConnections  = 32,
      maxOpenRequests = 32,
      maxRetries      = 0)


  def main(args: Array[String]): Unit = {
    // Load configuration parameters & environment

    val conf = MainConfig.loadFromConfig

    val threadPool = java.util.concurrent.Executors.newFixedThreadPool(conf.threadPoolCount)
    implicit val ec = ExecutionContext.fromExecutor(threadPool)

    Console.err.println(s"ES: ${conf.elasticSearchUrl}")
    Console.err.println(s"Kafka: ${conf.kafkaBroker}")
    Console.err.println(s"Schema Registry: ${conf.avroSchemaRegistryUrl}")
    Console.err.println(s"Phoenix: ${conf.phoenixUri}")

    val phoenix = PhoenixConnectionInfo(
      uri = conf.phoenixUri,
      user = conf.phoenixUser,
      pass = conf.phoenixPass)

    val activityWork = Future {
      val activityConnectors = Seq(AdminConnector(), CustomerConnector(), OrderConnector(), GiftCardConnector(),
        StoreCreditConnector())

      val activityProcessor = new ActivityProcessor(phoenix, activityConnectors)

      val avroProcessor = new AvroProcessor(
        schemaRegistryUrl = conf.avroSchemaRegistryUrl,
        processor = activityProcessor)

      val consumer = new MultiTopicConsumer(
        topics = Seq(conf.activityTopic),
        broker = conf.kafkaBroker,
        groupId = s"${conf.kafkaGroupId}_activity",
        processor = avroProcessor,
        startFromBeginning = conf.startFromBeginning)

      // Start consuming & processing
      println(s"Reading activities from broker ${conf.kafkaBroker}")

      consumer.readForever()
    }

    val trailWork = Future {
      val transformers = Map(
        "regions"                           → AvroTransformers.Region(),
        "countries"                         → AvroTransformers.Country(),
        "customers_search_view"             → AvroTransformers.CustomersSearchView(),
        "orders_search_view"                → AvroTransformers.OrdersSearchView(),
        "store_admins_search_view"          → AvroTransformers.StoreAdminsSearchView(),
        "failed_authorizations_search_view" → AvroTransformers.FailedAuthorizationsSearchView(),
        "store_credit_transactions_view"    → AvroTransformers.StoreCreditTransactionsView(),
        "skus"                              → AvroTransformers.Sku(),
        "gift_cards"                        → AvroTransformers.GiftCard(),
        "store_credits_search_view"         → AvroTransformers.StoreCreditsSearchView(),
        conf.connectionTopic                → ActivityConnectionTransformer(phoenix))

      val topicsPlusActivity = conf.kafkaTopics :+ conf.connectionTopic

      // Init processors & consumer
      val esProcessor = new ElasticSearchProcessor(
        uri = conf.elasticSearchUrl,
        cluster = conf.elasticSearchCluster,
        indexName = conf.elasticSearchIndex,
        topics = topicsPlusActivity,
        jsonTransformers = transformers)

      val avroProcessor = new AvroProcessor(
        schemaRegistryUrl = conf.avroSchemaRegistryUrl,
        processor = esProcessor)

      val consumer = new MultiTopicConsumer(
        topics = topicsPlusActivity,
        broker = conf.kafkaBroker,
        groupId = s"${conf.kafkaGroupId}_trail",
        processor = avroProcessor,
        startFromBeginning = conf.startFromBeginning)

      // Execture beforeAction
      println("Executing pre-consuming actions...")
      esProcessor.beforeAction()

      // Start consuming & processing
      println(s"Reading from broker ${conf.kafkaBroker}")
      consumer.readForever()
    }

    activityWork onFailure {
      case t ⇒ { 
        Console.err.println(s"Error occurred consuming activities: ${t.getMessage}")
        System.exit(1);
      }
    }

    trailWork onFailure {
      case t ⇒  { 
        Console.err.println(s"Error occurred indexing to ES: ${t}")
        System.exit(1);
      }
    }
    //These threads will actually never be ready.
    //This is a hedonist bot.
    Await.ready(activityWork, Duration.Inf)
    Await.ready(trailWork, Duration.Inf)
  }
}
