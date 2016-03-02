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
CustomerConnector, GiftCardConnector, SharedSearchConnector, StoreCreditConnector}
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
    maxConnections         : Int,
    startFromBeginning     : Boolean,
    doSetup                : Boolean)

object MainConfig {
  val environmentProperty = "env"
  val defaultEnvironment  = "default"

  def loadFromConfig : MainConfig = {

    val conf = ConfigFactory.load()
    val env = sys.props.getOrElse(environmentProperty, defaultEnvironment)
    println(s"Initializing Using $env Environment...")

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
      doSetup               = conf.getBoolean(s"$env.elastic.setup"))
  }
}

object Main {

  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()
  
  def main(args: Array[String]): Unit = {

    val conf = MainConfig.loadFromConfig

    if(conf.doSetup) setup(conf) else process(conf)
    System.exit(0);
  }

  def topicsPlusActivity(conf: MainConfig) = conf.kafkaTopics :+ conf.connectionTopic

  def transformers(conf: MainConfig, phoenix: PhoenixConnectionInfo)
  (implicit ec: ExecutionContext, cp: ConnectionPoolSettings) = 
    Map(
    "skus"                              → AvroTransformers.Sku(),
    "customers_search_view"             → AvroTransformers.CustomersSearchView(),
    "orders_search_view"                → AvroTransformers.OrdersSearchView(),
    "store_admins_search_view"          → AvroTransformers.StoreAdminsSearchView(),
    "gift_cards_search_view"            → AvroTransformers.GiftCardsSearchView(),
    "gift_card_transactions_view"       → AvroTransformers.GiftCardTransactionsView(),
    "store_credits_search_view"         → AvroTransformers.StoreCreditsSearchView(),
    "store_credit_transactions_view"    → AvroTransformers.StoreCreditTransactionsView(),
    "failed_authorizations_search_view" → AvroTransformers.FailedAuthorizationsSearchView(),
    "notes_search_view"                 → AvroTransformers.NotesSearchView(),
    "inventory_search_view"             → AvroTransformers.InventorySearchView(),
    conf.connectionTopic                → ActivityConnectionTransformer(phoenix))

  def phoenixConnection(conf: MainConfig) = 
    PhoenixConnectionInfo( uri = conf.phoenixUri, user = conf.phoenixUser,
      pass = conf.phoenixPass)

  def setup(conf: MainConfig) { 

    val threadPool = java.util.concurrent.Executors.newCachedThreadPool()
    implicit val ec = ExecutionContext.fromExecutor(threadPool)

    require(conf.doSetup == true)
    Console.err.println(s"Running Setup...")

    val phoenix = phoenixConnection(conf)

    implicit val connectionPoolSettings =
      ConnectionPoolSettings.create(system).copy(
        maxConnections  = conf.maxConnections,
        maxOpenRequests = conf.maxConnections,
        maxRetries      = 1)

    val esProcessor = new ElasticSearchProcessor(
      uri = conf.elasticSearchUrl,
      cluster = conf.elasticSearchCluster,
      indexName = conf.elasticSearchIndex,
      topics = topicsPlusActivity(conf),
      jsonTransformers = transformers(conf, phoenix))

    //Create ES mappings
    esProcessor.createMappings()

    Console.err.println(s"Done Running Setup...")
  }

  def process(conf: MainConfig) {
    require(conf.doSetup == false)

    val threadPool = java.util.concurrent.Executors.newCachedThreadPool()
    implicit val ec = ExecutionContext.fromExecutor(threadPool)

    Console.err.println(s"Running Green River...")

    implicit val connectionPoolSettings =
      ConnectionPoolSettings.create(system).copy(
        maxConnections  = conf.maxConnections,
        maxOpenRequests = conf.maxConnections,
        maxRetries      = 1)

    Console.err.println(s"ES: ${conf.elasticSearchUrl}")
    Console.err.println(s"Kafka: ${conf.kafkaBroker}")
    Console.err.println(s"Schema Registry: ${conf.avroSchemaRegistryUrl}")
    Console.err.println(s"Phoenix: ${conf.phoenixUri}")

    val phoenix = phoenixConnection(conf)

    val activityWork = Future {
        val activityConnectors = Seq(AdminConnector(), CustomerConnector(), 
          OrderConnector(), GiftCardConnector(), SharedSearchConnector(), StoreCreditConnector())

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

      // Init processors & consumer
      val esProcessor = new ElasticSearchProcessor(
        uri = conf.elasticSearchUrl,
        cluster = conf.elasticSearchCluster,
        indexName = conf.elasticSearchIndex,
        topics = topicsPlusActivity(conf),
        jsonTransformers = transformers(conf, phoenix))

      val avroProcessor = new AvroProcessor(
        schemaRegistryUrl = conf.avroSchemaRegistryUrl,
        processor = esProcessor)

      val consumer = new MultiTopicConsumer(
        topics = topicsPlusActivity(conf),
        broker = conf.kafkaBroker,
        groupId = s"${conf.kafkaGroupId}_trail",
        processor = avroProcessor,
        startFromBeginning = conf.startFromBeginning)

      // Start consuming & processing
      println(s"Reading from broker ${conf.kafkaBroker}")
      consumer.readForever()
    }

    activityWork onFailure {
      case t ⇒ {
        Console.err.println(s"Error consuming activities: ${t.getMessage}")
        System.exit(1);
      }
    }

    trailWork onFailure {
      case t ⇒  {
        Console.err.println(s"Error indexing to ES: $t")
        System.exit(1);
      }
    }
    //These threads will actually never be ready.
    //This is a hedonist bot.
    Await.ready(activityWork, Duration.Inf)
    Await.ready(trailWork, Duration.Inf)

  }

}
