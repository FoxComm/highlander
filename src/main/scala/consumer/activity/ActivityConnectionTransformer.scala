package consumer.activity

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.AvroJsonHelper
import consumer.elastic.AvroTransformer

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.postfixOps

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, HttpResponse}
import akka.util.ByteString
import akka.stream.{ActorMaterializer, Materializer}

import org.json4s.JsonAST.JInt
import org.json4s.jackson.JsonMethods.parse
import org.json4s.DefaultFormats

import consumer.elastic.JsonTransformer

import consumer.utils.PhoenixConnectionInfo
import consumer.utils.Phoenix
import consumer.utils.HttpResponseExtensions._
import akka.http.scaladsl.model.StatusCodes

final case class ActivityConnectionTransformer(conn: PhoenixConnectionInfo)
(implicit ec:ExecutionContext, mat: Materializer, ac: ActorSystem, cp: ConnectionPoolSettings) extends JsonTransformer { 

  implicit val formats: DefaultFormats.type = DefaultFormats

  def mapping = "activity_connections" as ()
  val phoenix = Phoenix(conn)

  def transform(json: String) : Future[String] = {

    Console.out.println(json)

    parse(json) \ "id" \ "int" match {
      case JInt(id) ⇒ queryPhoenixForConnection(id)
      case _ ⇒  throw new IllegalArgumentException("Activity connection is missing id")
    }
  }

  private def queryPhoenixForConnection(id: BigInt) : Future[String] = {
    val uri = s"connections/${id}"
    Console.err.println(uri)

    phoenix.get(uri).map { _.bodyText}
  }

}
