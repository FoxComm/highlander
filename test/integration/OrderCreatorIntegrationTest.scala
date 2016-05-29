import akka.http.scaladsl.model.StatusCodes

import models.activity.ActivityContext
import models.customer.{Customer, Customers}
import models.StoreAdmins
import models.order.Order
import models.product.SimpleContext
import models.objects._
import payloads.OrderPayloads.CreateOrder
import responses.order.FullOrder
import FullOrder.Root
import services.orders.OrderCreator
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._
import utils.seeds.Seeds
import Seeds.Factories
import cats.implicits._
import failures.NotFoundFailure400

class OrderCreatorIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import Extensions._

  implicit val ac = ActivityContext(userId = 1, userType = "b", transactionId = "c")

  "POST /v1/orders" - {
    "for an existing customer" - {
      "succeeds" in new Fixture {
        val payload  = CreateOrder(customerId = customer.id.some)
        val response = POST(s"v1/orders", payload)

        response.status must ===(StatusCodes.OK)
        val root = response.as[Root]
        root.customer.value.id must ===(customer.id)
        root.orderState must ===(Order.Cart)
      }

      "fails when the customer is not found" in new Fixture {
        val payload  = CreateOrder(customerId = 99.some)
        val response = POST(s"v1/orders", payload)

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(NotFoundFailure400(Customer, 99).description)
      }

      "returns current cart if customer already has one" in new Fixture {
        val payload = CreateOrder(customerId = customer.id.some)
        OrderCreator.createCart(storeAdmin, payload, productContext).futureValue
        val response = POST(s"v1/orders", payload)

        response.status must ===(StatusCodes.OK)
        val root = response.as[Root]
        root.customer.value.id must ===(customer.id)
        root.orderState must ===(Order.Cart)
      }
    }

    "for a new guest" - {
      "successfuly creates cart and new guest customer account" in new Fixture {
        val payload  = CreateOrder(email = "yax@yax.com".some)
        val response = POST(s"v1/orders", payload)
        val root     = response.as[Root]
        val guest    = root.customer.value

        response.status must ===(StatusCodes.OK)
        guest.isGuest mustBe true
        root.orderState must ===(Order.Cart)
        guest.id must !==(customer.id)
      }
    }

    "fails if neither a new guest or existing customer are provided" in {
      val payload  = CreateOrder()
      val response = POST(s"v1/orders", payload)

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===("customerId or email must be given")
    }
  }

  trait Fixture {
    val (productContext, storeAdmin, customer) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      customer       ← * <~ Customers.create(Factories.customer)
      storeAdmin     ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (productContext, storeAdmin, customer)).runTxn().futureValue.rightVal
  }
}
