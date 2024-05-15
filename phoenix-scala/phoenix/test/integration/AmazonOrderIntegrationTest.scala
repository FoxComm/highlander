import testutils._
import testutils.apis._
import testutils.fixtures._
import testutils.fixtures.api.ApiFixtureHelpers
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
    with TestActivityContext.AdminAC
    with ApiFixtureHelpers {

  "POST /v1/amazon-orders" - {
    "successfully creates amazonOrder from payload" in new Fixture {
      val created =
        AmazonOrders.findOneByAmazonOrderId(amazonOrderResponse.amazonOrderId).gimme.value
      created.id must === (amazonOrderResponse.id)
    }
  }

  "PATCH /v1/amazon-orders/:amazonOrderId" - {
    "update existing order" in new Fixture {
      val updatePayload = UpdateAmazonOrderPayload(orderStatus = "ChangedStatus")
      val updated = amazonOrdersApi(amazonOrderResponse.amazonOrderId)
        .update(updatePayload)
        .as[AmazonOrderResponse]
      updated.orderStatus must === (updatePayload.orderStatus)
    }
  }

  trait Fixture {
    val customer = api_newCustomer
    val amazonOrderPayload = CreateAmazonOrderPayload(
      amazonOrderId = "111-5296499-9653859",
      orderTotal = 4500,
      paymentMethodDetail = "CreditCard",
      orderType = "StandardOrder",
      currency = Currency.USD,
      orderStatus = "Shipped",
      purchaseDate = Instant.now,
      scope = LTree("1"),
      customerEmail = customer.email.value
    )

    val amazonOrderResponse = amazonOrdersApi.create(amazonOrderPayload).as[AmazonOrderResponse]
  }
}
