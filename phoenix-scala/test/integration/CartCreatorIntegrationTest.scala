import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.NotFoundFailure400
import models.customer.Customer
import payloads.OrderPayloads.CreateCart
import responses.cord.CartResponse
import services.carts.CartCreator
import util._
import util.fixtures.BakedFixtures

class CartCreatorIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/orders" - {
    "for an existing customer" - {
      "succeeds" in new Fixture {
        val response = cartsApi.create(CreateCart(customerId = customer.id.some))

        response.status must === (StatusCodes.OK)
        val root = response.as[CartResponse]
        root.customer.value.id must === (customer.id)
      }

      "fails when the customer is not found" in new Fixture {
        val response = cartsApi.create(CreateCart(customerId = 99.some))

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(Customer, 99).description)
      }

      "returns current cart if customer already has one" in new Fixture {
        CartCreator.createCart(storeAdmin, CreateCart(customerId = customer.id.some)).gimme
        val response = cartsApi.create(CreateCart(customerId = customer.id.some))

        response.status must === (StatusCodes.OK)
        val root = response.as[CartResponse]
        root.customer.value.id must === (customer.id)
      }
    }

    "for a new guest" - {
      "successfully creates cart and new guest customer account" in new Fixture {
        val response = cartsApi.create(CreateCart(email = "yax@yax.com".some))
        val root     = response.as[CartResponse]
        val guest    = root.customer.value

        response.status must === (StatusCodes.OK)
        guest.isGuest mustBe true
        guest.id must !==(customer.id)
      }
    }

    "fails if neither a new guest or existing customer are provided" in {
      val response = cartsApi.create(CreateCart())

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("customerId or email must be given")
    }
  }

  trait Fixture extends Customer_Seed with StoreAdmin_Seed
}
