import models.{Cart, Carts}
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

