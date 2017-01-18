package consumer.elastic

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import consumer.{ErrorTryAgainLater, JsonProcessor, PassthroughSource}
import consumer.elastic.mappings._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.client.transport.NoNodeAvailableException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.JsonAST._
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

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {
    // Find json transformer
    jsonTransformers get topic match {
      case Some(t) ⇒
        t.transform(inputJson).flatMap(outJson ⇒ indexJson(outJson, topic))
      case None ⇒
        Console.out.println(s"Skipping information from topic $topic")
        Future { () }
    }
  }

  //This save will index into several indices based on scope. Scopes are
  //constructured as a path with '.' seperating the elements. We want to index
  //based on the scope down the path. For example if the scope is "1.2.3" we will index
  //into 3 indices
  //
  //1.  admin_1.2.3
  //2.  admin_1.2
  //3.  admin_1
  private def indexJson(document: String, topic: String): Future[Unit] = {
    val json = parse(document)
    json \ "id" match {
      case JInt(jid) ⇒
        json \ "scope" match {
          case JString(scope) ⇒
            val scopes = distinctScopePaths(scope)
            indexScopes(scopes, jid, document, topic)

          //if no scope found, just save the good old way
          case _ ⇒ indexDocument(indexName, jid, document, topic)
        }
      case _ ⇒
        Console.out.println(s"Skipping unidentified document from topic $topic...\r\n$document")
        Future { () }
    }
  }

  //Produces a set of scopes by following up the tree
  //Example
  //input: "1.5.7.8"
  //output: ["1.5.7.8", "1.5.7", "1.5, "1"]
  private def distinctScopePaths(scope: String): Seq[String] = {
    val path = scope.split('.')
    (1 to path.length).map { idx ⇒
      path.slice(0, idx).mkString(".")
    }.distinct
  }

  private def indexScopes(scopes: Seq[String],
                          documentId: BigInt,
                          document: String,
                          topic: String): Future[Unit] = {
    Future
      .sequence(scopes.map { scope ⇒
        val scopedIndexName = s"${indexName}_${scope}"
        indexDocument(scopedIndexName, documentId, document, topic)
      })
      .map(_ ⇒ ())
  }

  private def indexDocument(scopedIndexName: String,
                            documentId: BigInt,
                            document: String,
                            topic: String): Future[Unit] = {
    Console.out.println(s"Scoped Indexing $topic into $scopedIndexName")
    val req = client.execute {
      index into scopedIndexName / topic id documentId doc PassthroughSource(document)
    }.map { _ ⇒
      ()
    }.recover {
      case e: RemoteTransportException if e.getCause.isInstanceOf[IndexNotFoundException] ⇒
        Console.out.println(s"Index $scopedIndexName not found, let's try later")
        throw ErrorTryAgainLater(s"Index $scopedIndexName not found")
    }

    req onFailure {
      case NonFatal(e) ⇒ Console.err.println(s"Error while indexing: $e")
    }

    req.map { _ ⇒
      ()
    }
  }
}
