import cats.implicits._
import failures.NotFoundFailure404
import models.account._
import payloads.OrderPayloads.CreateCart
import responses.cord.CartResponse
import services.carts.CartCreator
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

class CartCreatorIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/orders" - {
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
        CartCreator.createCart(storeAdmin, CreateCart(customerId = customer.accountId.some)).gimme

        cartsApi
          .create(CreateCart(customerId = customer.accountId.some))
          .as[CartResponse]
          .customer
          .value
          .id must === (customer.accountId)
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
