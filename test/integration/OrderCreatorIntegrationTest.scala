import akka.http.scaladsl.model.StatusCodes
import models.activity.ActivityContext
import models.{StoreAdmins, Order, Customer, Customers}
import models.product.{ProductContexts, SimpleContext}
import payloads.CreateOrder
import responses.FullOrder.Root
import services.orders.OrderCreator
import services.NotFoundFailure400
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import cats.implicits._

class OrderCreatorIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import Extensions._

  implicit val ac = ActivityContext(userId = 1, userType = "b", transactionId = "c")

  "POST /v1/orders" - {
    "for an existing customer" - {
      "succeeds" in new Fixture {
        val payload = CreateOrder(customerId = customer.id.some)
        val response = POST(s"v1/orders", payload)

        response.status must ===(StatusCodes.OK)
        val root = response.as[Root]
        root.customer.value.id must ===(customer.id)
        root.orderState must ===(Order.Cart)
      }

      "fails when the customer is not found" in new Fixture {
        val payload = CreateOrder(customerId = 99.some)
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
        val payload = CreateOrder(email = "yax@yax.com".some)
        val response = POST(s"v1/orders", payload)
        val root = response.as[Root]
        val guest = root.customer.value

        response.status must ===(StatusCodes.OK)
        guest.isGuest mustBe true
        root.orderState must ===(Order.Cart)
        guest.id must !==(customer.id)
      }
    }

    "fails if neither a new guest or existing customer are provided" in {
      val payload = CreateOrder()
      val response = POST(s"v1/orders", payload)

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===("customerId or email must be given")
    }
  }

  trait Fixture {
    val (productContext, storeAdmin, customer) = (for {
      productContext ← * <~ ProductContexts.create(SimpleContext.create)
      customer   ← * <~ Customers.create(Factories.customer)
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (productContext, storeAdmin, customer)).runTxn().futureValue.rightVal
  }
}

