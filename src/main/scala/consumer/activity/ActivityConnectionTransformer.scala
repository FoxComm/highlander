package consumer.activity

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

import consumer.AvroJsonHelper
import consumer.elastic.AvroTransformer

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
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

import consumer.elastic.JsonTransformer

final case class ActivityConnectionTransformer(phoenix: PhoenixConnectionInfo)
(implicit ec:ExecutionContext, mat: Materializer, ac: ActorSystem, cp: ConnectionPoolSettings) extends JsonTransformer { 

  def mapping = "activity_connections" as ()

  def transform(json: String) : String = {

    Console.out.println(json)

    parse(json) \ "id" \ "int" match {
      case JInt(id) ⇒ queryPhoenixForConnection(id)
      case _ ⇒  throw new IllegalArgumentException("Activity connection is missing id")
    }
  }

  def queryPhoenixForConnection(id: BigInt) : String = {
    val url = s"${phoenix.uri}/activities/connections/${id}"
    Console.err.println(url)

    get(url)
  }

  private def get(uri: String) : String = { 
    val request = HttpRequest(HttpMethods.GET,uri).addHeader(
      Authorization(BasicHttpCredentials(phoenix.user, phoenix.pass)))

    val f = Http().singleRequest(request, cp).flatMap{ 
      r ⇒ r.entity.toStrict(1.second).map(_.data.utf8String)
    }
    Await.result(f, 10 seconds)
  }
}
