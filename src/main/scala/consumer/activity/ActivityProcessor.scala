package consumer.activity

import java.time.Instant

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JNothing, JValue}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.{write ⇒ render}

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.stream.Materializer

import consumer.JsonProcessor
import consumer.AvroJsonHelper

import consumer.utils.PhoenixConnectionInfo
import consumer.utils.Phoenix
import consumer.utils.HttpResponseExtensions._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpResponse

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

final case class AppendActivity(activityId: Int, data: JValue)
final case class AppendNotification(sourceDimension: String, sourceObjectId: String, activityId: Int, data: JValue)

trait ActivityConnector {
  def process(offset: Long, activity: Activity): Future[Seq[Connection]]
}

final case class FailedToConnectActivity (
  activityId: Int,
  dimension: String,
  objectId: String,
  reason: String) 
  extends RuntimeException(s"Failed to connect activity ${activityId} to dimension '${dimension}' and object ${objectId} because: ${reason}")

final case class FailedToConnectNotification(
  activityId: Int,
  dimension: String,
  objectId: String,
  reason: String)
  extends RuntimeException(s"Failed to create notification for connection of activity $activityId to dimension " +
    s"'$dimension' and object $objectId because: $reason")

/**
 * This is a JsonProcessor which listens to the activity stream and processes the activity
 * using a sequence of activity connectors
 */
class ActivityProcessor(conn : PhoenixConnectionInfo, connectors: Seq[ActivityConnector])
(implicit ec: ExecutionContext, ac: ActorSystem, mat: Materializer, cp: ConnectionPoolSettings)
  extends JsonProcessor {

    implicit val formats: DefaultFormats.type = DefaultFormats

    val activityJsonFields = List("id", "activityType", "data", "context", "createdAt")
    val phoenix = Phoenix(conn)

    def beforeAction(){}

    def process(offset: Long, topic: String, inputJson: String): Future[Unit] = {

      val activityJson = AvroJsonHelper.transformJson(inputJson, activityJsonFields)
      val activity =  parse(activityJson).extract[Activity]

      Console.err.println()
      Console.err.println(s"Got Activity: ${activity.id}")

      val result = connectors.map { connector ⇒
        for {
          connections ← connector.process(offset, activity)
          responses ← process(connections)
        } yield responses
      }
      val responses = Future.sequence(result).map(_.flatten)

      //TODO check errors
      responses map { r ⇒  ()}
    }

    private def process(cs: Seq[Connection]) : Future[Seq[HttpResponse]] = { 
      Future.sequence(cs.map(connectUsingPhoenix))
    }

    private def connectUsingPhoenix(c: Connection) : Future[HttpResponse] = { 
      val uri = s"trails/${c.dimension}/${c.objectId}"
      Console.err.println(s"${uri}")

      //create append payload
      val append = AppendActivity(c.activityId, c.data)
      val body = render(append)

      //make request
      phoenix.post(uri, body).map{
        resp ⇒ 
          if(resp.status == StatusCodes.OK) {
            createPhoenixNotification(c, phoenix)
          } else {
            throw FailedToConnectActivity(c.activityId, c.dimension, c.objectId, resp.as[String])
          }
          resp
      }
    }

  private def createPhoenixNotification(conn: Connection, phoenix: Phoenix): Future[HttpResponse] = {
    val body = AppendNotification(
      sourceDimension = conn.dimension,
      sourceObjectId = conn.objectId,
      activityId = conn.activityId,
      data = JNothing)

    val notification = render(body)
    Console.err.println(s"POST notifications, $notification")

    phoenix.post("notifications", notification).map { response ⇒
      if (response.status != StatusCodes.OK) {
        throw new FailedToConnectNotification(conn.activityId, conn.dimension, conn.objectId, response.as[String])
      }
      response
    }
  }
}
