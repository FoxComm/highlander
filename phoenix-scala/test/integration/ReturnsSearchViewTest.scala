import models.returns.Returns
import testutils._
import utils.seeds.Seeds.Factories
import utils.db._
import testutils.TestSeeds
import testutils.fixtures.BakedFixtures

case class ReturnsSearchViewResult(
    id: Int,
    referenceNumber: String,
    state: String,
    placedAt: String,
    customer: String
)

class ReturnsSearchViewTest extends SearchViewTestBase with TestSeeds with BakedFixtures {

  type SearchViewResult = ReturnsSearchViewResult
  val searchViewName: String = "returns_search_view"
  val searchKeyName: String  = "reference_number"

  "smoke test search view" - new Fixture {
    "should compile" in {
      val view = findOneInSearchView(rma.refNum)
    }
  }

  trait Fixture extends StoreAdmin_Seed with Order_Baked {
    val rma = Returns
      .create(Factories.rma.copy(orderRef = order.refNum, accountId = customer.accountId))
      .gimme
  }
}
