package consumer

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
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
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()
  
  def main(args: Array[String]): Unit = {
    val conf = MainConfig.loadFromConfig()
    if (conf.doSetup) setup(conf) else process(conf)
    System.exit(0)
  }

  private def setup(conf: MainConfig): Unit = {
    val threadPool = java.util.concurrent.Executors.newCachedThreadPool()
    implicit val ec = ExecutionContext.fromExecutor(threadPool)

    require(conf.doSetup)
    Console.err.println(s"Running Setup...")

    implicit val connectionPoolSettings =
      ConnectionPoolSettings.create(system).copy(
        maxConnections  = conf.maxConnections,
        maxOpenRequests = conf.maxConnections,
        maxRetries      = 1)

    val esProcessor = new ElasticSearchProcessor(
      uri = conf.elasticSearchUrl,
      cluster = conf.elasticSearchCluster,
      indexName = conf.elasticSearchIndex,
      topics = conf.topicsPlusActivity(),
      jsonTransformers = Workers.topicTransformers(conf, conf.connectionInfo()))

    // Create ES mappings
    esProcessor.createMappings()
    Console.out.println(s"Done running setup...")
  }

  private def process(conf: MainConfig): Unit = {
    require(!conf.doSetup)

    val threadPool = java.util.concurrent.Executors.newCachedThreadPool()
    implicit val ec = ExecutionContext.fromExecutor(threadPool)

    implicit val connectionPoolSettings =
      ConnectionPoolSettings.create(system).copy(
        maxConnections  = conf.maxConnections,
        maxOpenRequests = conf.maxConnections,
        maxRetries      = 1)

    Console.out.println(s"Running Green River...")
    Console.out.println(s"ES: ${conf.elasticSearchUrl}")
    Console.out.println(s"Kafka: ${conf.kafkaBroker}")
    Console.out.println(s"Schema Registry: ${conf.avroSchemaRegistryUrl}")
    Console.out.println(s"Phoenix: ${conf.phoenixUri}")

    val connectionInfo = conf.connectionInfo()
    val activityWork = Workers.activityWorker(conf, connectionInfo)
    val searchViewWork = Workers.searchViewWoker(conf, connectionInfo)

    activityWork.onFailure { case t ⇒
      Console.err.println(s"Error consuming activities: ${t.getMessage}")
      System.exit(1)
    }

    searchViewWork.onFailure { case t ⇒
      Console.err.println(s"Error indexing to ES: $t")
      System.exit(1)
    }

    // These threads will actually never be ready.
    // This is a hedonist bot.
    Await.ready(activityWork, Duration.Inf)
    Await.ready(searchViewWork, Duration.Inf)
  }
}
