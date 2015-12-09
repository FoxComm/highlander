package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext

import com.sksamuel.elastic4s.{EdgeNGramTokenFilter, LowercaseTokenFilter, StandardTokenizer,
CustomAnalyzerDefinition, ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._

import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.indices.IndexMissingException

import org.json4s.JsonAST.{JValue, JInt, JObject, JField, JString}
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats

import consumer.JsonProcessor
import consumer.AvroJsonHelper

final case class ActivityContext(
  userId: Int,
  userType: String,
  transactionId: String)

final case class Activity(
  id: Int = 0, 
  activityType: String, 
  data: JValue, 
  context: ActivityContext,
  createdAt: Instant = Instant.now)

final case class Connection(
  dimension: String,
  objectId: String,
  data: JValue,
  activityId: Int)

trait ActivityConnector {
  def process(offset: Long, activity: Activity)(implicit ec: ExecutionContext) : Seq[Connection]
}

/**
 * This is a JsonProcessor which listens to the activity stream and processes the activity
 * using a sequence of activity connectors
 */
class ActivityProcessor(phoenixUri: String, connectors: Seq[ActivityConnector])
  extends JsonProcessor {

    implicit val formats: DefaultFormats.type = DefaultFormats

    val activityJsonFields = Map(
      "id" → "id", 
      "activity_type" → "activityType", 
      "data" → "data", 
      "context" → "context",
      "created_at" → "createdAt")

    def beforeAction()(implicit ec: ExecutionContext) {}

    def process(offset: Long, topic: String, inputJson: String)(implicit ec: ExecutionContext): Unit = {
      Console.err.println(s"${topic} ${offset}: ${inputJson}")
      val activityJson = AvroJsonHelper.transformJson(inputJson, activityJsonFields)
      val activity =  parse(activityJson).extract[Activity]

      val connections = connectors.flatMap(_.process(offset, activity))
    }

}
