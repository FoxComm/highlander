package consumer.elastic

import scala.collection.mutable.Buffer
import scala.concurrent.Future

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import consumer.MainConfig.IndexTopicMap
import consumer.aliases._
import consumer.elastic.mappings._
import consumer.{AvroJsonHelper, JsonProcessor}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.client.transport.NoNodeAvailableException
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

final case class Scope(id: Int = 0, parentPath: Option[String]) {
  def path = parentPath match {
    case Some(pp) ⇒ if (pp.isEmpty) s"$id" else s"$pp.$id"
    case None     ⇒ s"$id"
  }
}

/**
  * This is a JsonProcessor which listens to scope creation and creates ES mappings
  * for the new scope.
  */
class ScopeProcessor(uri: String,
                     cluster: String,
                     indexTopics: IndexTopicMap,
                     jsonTransformers: Map[String, JsonTransformer])(
    implicit ec: EC, ac: AS, mat: AM, cp: CP, sc: SC)
    extends JsonProcessor {

  implicit val formats: DefaultFormats.type = DefaultFormats

  val settings        = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client          = ElasticClient.transport(settings, ElasticsearchClientUri(uri))
  val scopeJsonFields = List("id", "parentPath")

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {

    val scopeJson = AvroJsonHelper.transformJson(inputJson, scopeJsonFields)
    val scope     = parse(scopeJson).extract[Scope]

    Console.err.println()
    Console.err.println(s"Got Scope ${scope.path}")

    createIndex(scope)
  }

  private def createIndex(scope: Scope): Future[Unit] = {
    var createFutures = Buffer[Future[Unit]]()
    indexTopics.foreach {
      case (indexName, topics) ⇒ {
          val scopedIndexName = s"${indexName}_${scope.path}"
          Console.out.println(s"Creating type mappings for index: ${scopedIndexName}")
          //get json mappings for topics
          val jsonMappings = jsonTransformers.filter {
            case (key, _) ⇒ topics.contains(key)
          }.mapValues(_.mapping()).values.toSeq

          // create index with scope in the
          createFutures += client.execute {
            create index scopedIndexName mappings (jsonMappings: _*) analysis (autocompleteAnalyzer, lowerCasedAnalyzer, upperCasedAnalyzer)
          }.map { _ ⇒
            ()
          }.recover {
            case e: RemoteTransportException
                if e.getCause.isInstanceOf[IndexAlreadyExistsException] ⇒
              Console.out.println(s"Index $scopedIndexName already exists, skip")
          }
        }
    }
    Future.sequence(createFutures).map(_ ⇒ ())
  }
}
