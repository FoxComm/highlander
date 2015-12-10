package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

import org.json4s.JsonAST.{JValue, JInt, JObject, JField, JString}
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.{write ⇒ render}

import cats.std.future._

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.util.ByteString
import akka.stream.{ActorMaterializer, Materializer}

import consumer.JsonProcessor
import consumer.AvroJsonHelper

import scala.language.postfixOps

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

final case class AppendActivity(activityId: Int, data: JValue)

trait ActivityConnector {
  def process(offset: Long, activity: Activity)(implicit ec: ExecutionContext) : Seq[Connection]
}

final case class PhoenixConnectionInfo(
  uri: String,
  user: String,
  pass: String)

/**
 * This is a JsonProcessor which listens to the activity stream and processes the activity
 * using a sequence of activity connectors
 */
class ActivityProcessor(phoenix : PhoenixConnectionInfo, connectors: Seq[ActivityConnector])
  extends JsonProcessor {

    implicit val formats: DefaultFormats.type = DefaultFormats
    implicit val system = ActorSystem("system")
    implicit val materializer = ActorMaterializer()


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

    private def process(cs: Seq[Connection]) { 
      cs.foreach(connectUsingPhoenix)
    }

    private def connectUsingPhoenix(c: Connection) { 
      val uri = s"${phoenix.uri}/trails/${c.dimension}/${c.objectId}"
      Console.err.println(s"${uri}")

      //create append payload
      val append = AppendActivity(c.activityId, c.data)
      val body = render(append)

      //make request
      post(uri, body)

      //TODO, check request and report
    }

    
    lazy final val connectionPoolSettings = 
      ConnectionPoolSettings.create(implicitly[ActorSystem]).copy(
      maxConnections  = 32,
      maxOpenRequests = 32,
      maxRetries      = 0)

    private def post(uri: String, body: String) : HttpResponse = { 
      val request = HttpRequest(
        method = HttpMethods.POST,
        uri    = uri,
        entity = HttpEntity.Strict(
          ContentTypes.`application/json`,
          ByteString(body)
        )).addHeader(Authorization(BasicHttpCredentials(phoenix.user, phoenix.pass)))

      val post = Http().singleRequest(request, connectionPoolSettings)
      Await.result(post, 10 seconds)
    }
}
