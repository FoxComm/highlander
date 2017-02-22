import models.returns.{Return, Returns}
import org.json4s.JObject
import testutils._
import utils.seeds.Seeds.Factories
import utils.db._
import testutils.TestSeeds
import testutils.fixtures.BakedFixtures
import cats.implicits._
import models.returns.Return.ReturnType

case class ReturnsSearchViewResult(
    id: Int,
    referenceNumber: String,
    orderId: Int,
    orderRef: String,
    state: Return.State,
    messageToAccount: Option[String],
    returnType: ReturnType,
    customer: JObject
)

class ReturnsSearchViewTest extends SearchViewTestBase with TestSeeds with BakedFixtures {

  type SearchViewResult = ReturnsSearchViewResult
  val searchViewName: String = "returns_search_view"
  val searchKeyName: String  = "id"

  "smoke test search view" - {
    "should work against fixture return" in new Fixture {
      val rmaSearchView = findOneInSearchView(rma.id)

      {
        import rmaSearchView._

        id must === (rma.id)
        referenceNumber must === (rma.refNum)
        orderId must === (rma.orderId)
        orderRef must === (rma.orderRef)
        state must === (rma.state)
        messageToAccount must === (rma.messageToAccount)
        returnType must === (rma.returnType)
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed with Order_Baked {
    val rma = Returns
      .create(Factories.rma.copy(orderRef = order.refNum, accountId = customer.accountId))
      .gimme
  }
}
