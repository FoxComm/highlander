import cats.implicits._
import core.failures.NotFoundFailure404
import phoenix.models.account._
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.responses.cord.CartResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

class CartCreatorIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/carts" - {
    "for an existing customer" - {
      "succeeds" in new Fixture {
        cartsApi
          .create(CreateCart(customerId = customer.accountId.some))
          .as[CartResponse]
          .customer
          .value
          .id must === (customer.accountId)
      }

      "fails when the customer is not found" in new Fixture {
        // FIXME: should be 400
        cartsApi
          .create(CreateCart(customerId = 99.some))
          .mustFailWith404(NotFoundFailure404(User, 99))
      }

      "returns current cart if customer already has one" in new Fixture {
        val refNum = cartsApi
          .create(CreateCart(customerId = customer.accountId.some))
          .as[CartResponse]
          .referenceNumber

        cartsApi
          .create(CreateCart(customerId = customer.accountId.some))
          .as[CartResponse]
          .referenceNumber must === (refNum)
      }
    }

    "for a new guest" - {
      "successfully creates cart and new guest customer account" in new Fixture {
        val guest =
          cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].customer.value

        guest.isGuest mustBe true
        guest.id must !==(customer.accountId)
      }
    }

    "fails if neither a new guest or existing customer are provided" in {
      cartsApi.create(CreateCart()).mustFailWithMessage("customerId or email must be given")
    }
  }

  trait Fixture extends Customer_Seed with StoreAdmin_Seed
}
