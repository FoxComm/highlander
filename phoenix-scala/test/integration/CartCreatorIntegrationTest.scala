import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.NotFoundFailure400
import models.account._
import payloads.OrderPayloads.CreateCart
import responses.cord.CartResponse
import services.carts.CartCreator
import util._
import util.fixtures.BakedFixtures
import failures.NotFoundFailure404

class CartCreatorIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/orders" - {
    "for an existing customer" - {
      "succeeds" in new Fixture {
        val payload  = CreateCart(customerId = customer.accountId.some)
        val response = POST(s"v1/orders", payload)

        response.status must === (StatusCodes.OK)
        val root = response.as[CartResponse]
        root.customer.value.id must === (customer.accountId)
      }

      "fails when the customer is not found" in new Fixture {
        val payload  = CreateCart(customerId = 99.some)
        val response = POST(s"v1/orders", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure404(User, 99).description)
      }

      "returns current cart if customer already has one" in new Fixture {
        val payload = CreateCart(customerId = customer.accountId.some)
        CartCreator.createCart(storeAdmin, payload).gimme
        val response = POST(s"v1/orders", payload)

        response.status must === (StatusCodes.OK)
        val root = response.as[CartResponse]
        root.customer.value.id must === (customer.accountId)
      }
    }

    "for a new guest" - {
      "successfully creates cart and new guest customer account" in new Fixture {
        val payload  = CreateCart(email = "yax@yax.com".some)
        val response = POST(s"v1/orders", payload)
        val root     = response.as[CartResponse]
        val guest    = root.customer.value

        response.status must === (StatusCodes.OK)
        guest.isGuest mustBe true
        guest.id must !==(customer.accountId)
      }
    }

    "fails if neither a new guest or existing customer are provided" in {
      val payload  = CreateCart()
      val response = POST(s"v1/orders", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("customerId or email must be given")
    }
  }

  trait Fixture extends Customer_Seed with StoreAdmin_Seed
}
