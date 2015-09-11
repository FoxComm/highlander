import akka.http.scaladsl.model.StatusCodes
import models.{Order, Customer, Customers}
import payloads.CreateOrder
import responses.FullOrder.Root
import services.{CustomerHasCart, OrderCreator, NotFoundFailure}
import util.IntegrationTestBase
import utils.Seeds.Factories
import util.SlickSupport.implicits._

class OrderCreatorIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import Extensions._

  "POST /v1/orders" - {
    "succeeds" in new Fixture {
      val payload = CreateOrder(customerId = customer.id)
      val response = POST(s"v1/orders", payload)

      response.status must ===(StatusCodes.OK)
      val root = response.as[Root]
      root.customer.get.id must ===(customer.id)
      root.orderStatus must ===(Order.Cart)
    }

    "fails when the customer is not found" in new Fixture {
      val payload = CreateOrder(customerId = 99)
      val response  = POST(s"v1/orders", payload)

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(NotFoundFailure(Customer, 99).description)
    }

    "fails when the customer already has a cart" in new Fixture {
      val payload = CreateOrder(customerId = customer.id)
      OrderCreator.createCart(payload).futureValue
      val response  = POST(s"v1/orders", payload)

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(CustomerHasCart(customer.id).description)
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).futureValue
  }
}

