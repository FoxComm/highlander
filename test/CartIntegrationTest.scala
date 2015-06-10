import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import models.{Cart, Carts}
import org.json4s.DefaultFormats
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{MustMatchers, Suite, fixture, SuiteMixin, FreeSpec, OneInstancePerTest, Outcome}
import util.DbTestSupport

import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


/**
 * The Server is shut down by shutting down the ActorSystem
 */
class CartIntegrationTest extends FreeSpec
  with MustMatchers
  with DbTestSupport
  with HttpSupport
  with ScalaFutures {

  import concurrent.ExecutionContext.Implicits.global

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = Span(5, Seconds),
    interval = Span(20, Milliseconds)
  )

  import Extensions._
  import api._

  import org.json4s.jackson.JsonMethods._
  implicit val formats = DefaultFormats

  "returns new items" in {
    val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = None)).futureValue

    val response = postJson(
      s"v1/cart/$cartId/line-items",
       """
         | [ { "skuId": 1, "quantity": 1 },
         |   { "skuId": 5, "quantity": 2 } ]
       """.stripMargin)

    val responseBody = response.bodyText
    val ast = parse(responseBody)

    val skuIds = ast.children.map(c ⇒ (c \ "skuId").extract[Int])
    skuIds.sortBy(identity) mustBe List(1, 5, 5)
  }
}

