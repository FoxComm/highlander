import models._
import org.json4s.DefaultFormats
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import util.DbTestSupport

/**
 * The Server is shut down by shutting down the ActorSystem
 */
class CartIntegrationTest extends FreeSpec
  with MustMatchers
  with DbTestSupport
  with HttpSupport
  with AutomaticAuth
  with ScalaFutures {

  import concurrent.ExecutionContext.Implicits.global

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = Span(5, Seconds),
    interval = Span(20, Milliseconds)
  )

  import Extensions._
  import org.json4s.jackson.JsonMethods._
  implicit val formats = DefaultFormats

  "returns new items" in {
    val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = None)).futureValue

    val response = POST(
      s"v1/carts/$cartId/line_items",
       """
         | [ { "skuId": 1, "quantity": 1 },
         |   { "skuId": 5, "quantity": 2 } ]
       """.stripMargin)

    val responseBody = response.bodyText
    val ast = parse(responseBody)

    val lineItems = (ast \ "lineItems").extract[List[LineItem]]
    lineItems.map(_.skuId).sortBy(identity)  mustBe List(1, 5, 5)
  }

  "deletes line items" in {
    val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = None)).futureValue
    val seedLineItems = (1 to 2).map { _ => LineItem(id = 0, parentId = cartId, parentType = "cart", skuId = 1) }
    db.run(LineItems.returningId ++= seedLineItems.toSeq).futureValue

    val response = DELETE(s"v1/carts/$cartId/line_items/1")
    val responseBody = response.bodyText
    val ast = parse(responseBody)

    val lineItems = (ast \ "lineItems").extract[List[LineItem]]
    lineItems mustBe List(LineItem(id = 2, parentId = cartId, parentType = "cart", skuId = 1))
  }
}

