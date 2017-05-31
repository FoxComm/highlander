import testutils._
import testutils.apis._
import testutils.fixtures._
import phoenix.payloads.AmazonOrderPayloads._
import phoenix.responses.cord.AmazonOrderResponse
import phoenix.models.cord._
import core.failures._
import core.db._
import cats.implicits._
import phoenix.utils.seeds.Factories
import core.utils.Money.Currency
import java.time.Instant
import com.github.tminglei.slickpg.LTree

class AmazonOrderIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with BakedFixtures
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC {

  "POST /v1/amazon_orders" - {
    "successfully creates amazonOrder from payload" in new Customer_Seed {
      val payload = CreateAmazonOrderPayload(amazonOrderId = "111-5296499-9653859",
                                             orderTotal = 4500,
                                             paymentMethodDetail = "CreditCard",
                                             orderType = "StandardOrder",
                                             currency = Currency.USD,
                                             orderStatus = "Shipped",
                                             purchaseDate = Instant.now,
                                             scope = LTree("1"),
                                             customerEmail = customer.email.value)

      val root    = amazonOrderApi.create(payload).as[AmazonOrderResponse.Root]
      val created = AmazonOrders.findOneByAmazonOrderId(root.amazonOrderId).gimme.value
      created.id must === (root.id)
    }
  }

  "PATCH /v1/amazon_orders/:amazonOrderId" - {
    "update existing order" in new Fixture {
      val updPayload = UpdateAmazonOrderPayload(orderStatus = "ChangedStatus")
      val updated =
        amazonOrderApi.update(amazonOrder.amazonOrderId, updPayload).as[AmazonOrderResponse.Root]
      updated.orderStatus must === (updPayload.orderStatus)
    }
  }

  trait Fixture extends Customer_Seed {
    val amazonOrder =
      AmazonOrders.create(Factories.amazonOrder.copy(accountId = customer.accountId)).gimme
  }
}
