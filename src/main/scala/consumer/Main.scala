package consumer

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalacache.ScalaCache
import scalacache.lrumap.LruMapCache
import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.ActorMaterializer

import consumer.elastic.ElasticSearchProcessor

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
  implicit val system       = ActorSystem("system")
  implicit val materializer = ActorMaterializer()
  implicit val scalaCache   = ScalaCache(LruMapCache(1))

  def main(args: Array[String]): Unit = {
    val conf = MainConfig.loadFromConfig()
    if (conf.doSetup) setup(conf) else process(conf)
    System.exit(0)
  }

  private def setup(conf: MainConfig): Unit = {
    val threadPool  = java.util.concurrent.Executors.newCachedThreadPool()
    implicit val ec = ExecutionContext.fromExecutor(threadPool)

    require(conf.doSetup)
    Console.out.println(s"Running Elasticsearch setup...")
    Console.out.println(s"Host: ${conf.elasticSearchUrl}")
    Console.out.println(s"Cluster: ${conf.elasticSearchCluster}")

    implicit val connectionPoolSettings: ConnectionPoolSettings = ConnectionPoolSettings
      .default(implicitly[ActorSystem])
      .withMaxConnections(conf.maxConnections)
      .withMaxOpenRequests(conf.maxConnections)
      .withMaxRetries(1)

    val esProcessor = new ElasticSearchProcessor(
        uri = conf.elasticSearchUrl,
        cluster = conf.elasticSearchCluster,
        indexName = conf.elasticSearchIndex,
        topics = conf.topicsPlusActivity(),
        jsonTransformers = Workers.topicTransformers(conf, conf.connectionInfo()))

    // Create ES mappings
    esProcessor.createMappings()
    Console.out.println(s"Done")
  }

  private def process(conf: MainConfig): Unit = {
    require(!conf.doSetup)

    val threadPool  = java.util.concurrent.Executors.newCachedThreadPool()
    implicit val ec = ExecutionContext.fromExecutor(threadPool)

    implicit val connectionPoolSettings: ConnectionPoolSettings = ConnectionPoolSettings
      .default(implicitly[ActorSystem])
      .withMaxConnections(conf.maxConnections)
      .withMaxOpenRequests(conf.maxConnections)
      .withMaxRetries(1)

    Console.out.println(s"Running Green River...")
    Console.out.println(s"ES: ${conf.elasticSearchUrl}")
    Console.out.println(s"Kafka: ${conf.kafkaBroker}")
    Console.out.println(s"Schema Registry: ${conf.avroSchemaRegistryUrl}")
    Console.out.println(s"Phoenix: ${conf.phoenixUri}")

    val connectionInfo    = conf.connectionInfo()
    val activityWork      = Workers.activityWorker(conf, connectionInfo)
    val searchViewWorkers = Workers.searchViewWorkers(conf, connectionInfo)

    activityWork.onFailure {
      case t ⇒
        Console.err.println(s"Error consuming activities: ${t.getMessage}")
        System.exit(1)
    }

    searchViewWorkers.onFailure {
      case t ⇒
        Console.err.println(s"Error indexing to ES: $t")
        System.exit(1)
    }

    // These threads will actually never be ready.
    // This is a hedonist bot.
    Await.ready(activityWork, Duration.Inf)
    Await.ready(searchViewWorkers, Duration.Inf)
  }
}
