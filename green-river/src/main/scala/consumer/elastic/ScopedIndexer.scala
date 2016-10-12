package consumer.elastic

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import consumer.JsonProcessor
import consumer.PassthroughSource
import consumer.elastic.mappings._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.client.transport.NoNodeAvailableException
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.JsonAST.JInt
import org.json4s.JsonAST.JString
import org.json4s.jackson.JsonMethods.parse

/**
  * This is a JsonProcessor which processes json and indexs it into elastic search.
  * It calls a json transform function before sending it to elastic search.
  *
  * If the json has a {"id" : <id>} field after transformation, it extracts that
  * id and uses it as the _id in elasticsearch for that item. This is important so that
  * we don't duplicate entries in ES.
  */
class ScopedIndexer(uri: String,
                    cluster: String,
                    indexName: String,
                    topics: Seq[String],
                    jsonTransformers: Map[String, JsonTransformer])(implicit ec: ExecutionContext)
    extends JsonProcessor {

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client   = ElasticClient.transport(settings, ElasticsearchClientUri(uri))

  def process(offset: Long, topic: String, inputJson: String): Future[Unit] = {
    // Find json transformer
    jsonTransformers get topic match {
      case Some(t) ⇒
        t.transform(inputJson).map { outJson ⇒
          save(outJson, topic)
        }
      case None ⇒
        Console.out.println(s"Skipping information from topic $topic")
        Future { () }
    }
  }

  private def save(document: String, topic: String): Future[Unit] = {
    // See if it has an id and use that as _id in elasticsearch.
    val json = parse(document)
    json \ "id" match {
      case JInt(jid) ⇒
        json \ "scope" match {
          case JString(scope) ⇒ {
              val scopedIndexName = s"${indexName}_${scope}"
              save(scopedIndexName, jid, document, topic)
            }
          //if no scope found, just save the good old way
          case _ ⇒ save(indexName, jid, document, topic)
        }
      case _ ⇒
        Console.out.println(s"Skipping unidentified document from topic $topic...\r\n$document")
        Future { () }
    }
  }

  private def save(scopedIndexName: String,
                   documentId: BigInt,
                   document: String,
                   topic: String): Future[Unit] = {
    val req = client.execute {
      index into scopedIndexName / topic id documentId doc PassthroughSource(document)
    }

    req onFailure {
      case NonFatal(e) ⇒ Console.err.println(s"Error while indexing: $e")
    }

    req.map { r ⇒
      ()
    }
  }
}
