package consumer.elastic

import consumer.AvroJsonHelper

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import consumer.JsonProcessor
import consumer.PassthroughSource
import consumer.elastic.mappings._
import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.client.transport.NoNodeAvailableException
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.JsonAST.JInt
import org.json4s.jackson.JsonMethods.parse

/**
  * This is a JsonProcessor which processes json and indexs it into elastic search.
  * It calls a json transform function before sending it to elastic search.
  *
  * If the json has a {"id" : <id>} field after transformation, it extracts that
  * id and uses it as the _id in elasticsearch for that item. This is important so that
  * we don't duplicate entries in ES.
  */
class ObjectSchemaProcessor(uri: String, cluster: String, schemasTopic: String)(
    implicit ec: ExecutionContext)
    extends JsonProcessor {

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client   = ElasticClient.transport(settings, ElasticsearchClientUri(uri))

  private val futureUnit: Future[Unit] = Future { () }

  def process(offset: Long, topic: String, key: String, inputJson: String): Future[Unit] = {
//    val parsed = AvroJsonHelper.transformJson(inputJson)
//    val parsedRaw = AvroJsonHelper.transformJsonRaw(inputJson)
//    Console.out.println(s"SCHEMAS: topic = $topic, document = $inputJson")
//    Console.out.println(s"SCHEMAS: parsed = $parsed")

//    client.execute {
//      put mapping "admin" / "products_search_view" fields(
//        field("external_id").typed(IntegerType)
//      )
//    }


    futureUnit
  }
}
