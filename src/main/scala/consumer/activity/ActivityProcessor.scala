package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext

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
  objectId: BigInt,
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
      val activityJson = AvroJsonHelper.transformJson(inputJson, activityJsonFields)

      val activity =  parse(activityJson).extract[Activity]
      Console.err.println(s"Got Activity: ${activity.id}")
      val connections = connectors.flatMap(_.process(offset, activity))

      process(connections)
    }

    def process(cs: Seq[Connection]) { 
      cs.foreach(connectUsingPhoenix)
    }

    def connectUsingPhoenix(c: Connection) { 
      //This is where we call the connect endpoint in phoenix
      Console.err.println(s"Connecting activity ${c.activityId} to (${c.dimension}, ${c.objectId})")
    }

}
