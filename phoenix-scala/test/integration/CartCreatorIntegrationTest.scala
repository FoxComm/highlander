import Extensions._
import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import failures.NotFoundFailure400
import models.StoreAdmins
import models.customer.{Customer, Customers}
import payloads.OrderPayloads.CreateCart
import responses.cord.CartResponse
import services.carts.CartCreator
import util._
import utils.db._
import utils.seeds.Seeds.Factories

import scala.concurrent.ExecutionContext.Implicits.global

class CartCreatorIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with TestActivityContext.AdminAC {

  "POST /v1/orders" - {
    "for an existing customer" - {
      "succeeds" in new Fixture {
        val payload  = CreateCart(customerId = customer.id.some)
        val response = POST(s"v1/orders", payload)

        response.status must === (StatusCodes.OK)
        val root = response.as[CartResponse]
        root.customer.value.id must === (customer.id)
      }

      "fails when the customer is not found" in new Fixture {
        val payload  = CreateCart(customerId = 99.some)
        val response = POST(s"v1/orders", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(Customer, 99).description)
      }

      "returns current cart if customer already has one" in new Fixture {
        val payload = CreateCart(customerId = customer.id.some)
        CartCreator.createCart(storeAdmin, payload).gimme
        val response = POST(s"v1/orders", payload)

        response.status must === (StatusCodes.OK)
        val root = response.as[CartResponse]
        root.customer.value.id must === (customer.id)
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
        guest.id must !==(customer.id)
      }
    }

    "fails if neither a new guest or existing customer are provided" in {
      val payload  = CreateCart()
      val response = POST(s"v1/orders", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("customerId or email must be given")
    }
  }

  trait Fixture {
    val (storeAdmin, customer) = (for {
      customer   ← * <~ Customers.create(Factories.customer)
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (storeAdmin, customer)).gimme
  }
}
