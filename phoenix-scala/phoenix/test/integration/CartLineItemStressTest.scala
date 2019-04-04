import java.time.Instant

import org.scalatest.concurrent.TimeLimits
import org.scalatest.time.SpanSugar._
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api._

class CartLineItemStressTest
    extends IntegrationTestBase
    with TimeLimits
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with ApiFixtures
    with ApiFixtureHelpers {

  "Cart line item updater should tolerate" - {

    "big line item qty delta" in {
//      println("test")
//      println(Instant.now())
      val f = new fixture()
//      println(Instant.now())
      failAfter(5 seconds) {
//        println(Instant.now())
        f.addToCart(500)
//        println(Instant.now())
      }
    }

    "frequent changes to line item qty" in {
      val f = new fixture
      failAfter(1 second) {
        List.fill(10)(1).par.foreach(f.addToCart)
      }
    }

    "both combined" in {
      val f = new fixture()
      failAfter(1 second) {
        List.fill(10)(500).par.foreach(f.addToCart)
      }
    }
  }

  private class fixture {
    private val cartRef = api_newCustomerCart(api_newCustomer().id).referenceNumber
    private val skuCode = ProductSku_ApiFixture().skuCode

    def addToCart(qty: Int): Unit =
      cartsApi(cartRef).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 500))).mustBeOk()

    // TODO update/override quantities test
  }

}
