import akka.http.scaladsl.model.StatusCodes
import models.{Order, Customer, Customers}
import payloads.CreateOrder
import responses.FullOrder.Root
import services.orders.OrderCreator
import services.CartFailures.CustomerHasCart
import services.NotFoundFailure404
import util.IntegrationTestBase
import utils.Seeds.Factories
import util.SlickSupport.implicits._
import cats.implicits._

class OrderCreatorIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import Extensions._

  "POST /v1/orders" - {
    "for an existing customer" - {
      "succeeds" in new Fixture {
        val payload = CreateOrder(customerId = customer.id.some)
        val response = POST(s"v1/orders", payload)

        response.status must ===(StatusCodes.OK)
        val root = response.as[Root]
        root.customer.value.id must ===(customer.id)
        root.orderStatus must ===(Order.Cart)
      }

      "fails when the customer is not found" in new Fixture {
        val payload = CreateOrder(customerId = 99.some)
        val response = POST(s"v1/orders", payload)

        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(NotFoundFailure404(Customer, 99).description)
      }

      "fails when the customer already has a cart" in new Fixture {
        val payload = CreateOrder(customerId = customer.id.some)
        OrderCreator.createCart(payload).futureValue
        val response = POST(s"v1/orders", payload)

        response.status must ===(StatusCodes.BadRequest)
        response.errors must ===(CustomerHasCart(customer.id).description)
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
        root.orderStatus must ===(Order.Cart)
        guest.id must !==(customer.id)
      }
    }

    "fails if neither a new guest or existing customer are provided" in {
      val payload = CreateOrder()
      val response = POST(s"v1/orders", payload)

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(List("customerId or email must be given"))
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).futureValue
  }
}

