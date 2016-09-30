import akka.http.scaladsl.model.HttpResponse

import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import payloads.CustomerPayloads._
import payloads.PaymentPayloads._
import util._

trait PhoenixAdminApi extends HttpSupport {
  this: Suite with PatienceConfiguration with DbTestSupport ⇒

  private val adminPath = "v1"

  object customerAPI {
    private val customerPath = s"$adminPath/customers"

    def create(payload: CreateCustomerPayload): HttpResponse =
      POST(customerPath, payload)

    def get(customerId: Int): HttpResponse =
      GET(s"$customerPath/$customerId")

    def update(customerId: Int, payload: UpdateCustomerPayload): HttpResponse =
      PATCH(s"$customerPath/$customerId", payload)

    def activate(customerId: Int, payload: ActivateCustomerPayload): HttpResponse =
      POST(s"$customerPath/$customerId/activate", payload)

    def disable(customerId: Int, payload: ToggleCustomerDisabled): HttpResponse =
      POST(s"$customerPath/$customerId/disable", payload)

    def blacklist(customerId: Int, payload: ToggleCustomerBlacklisted): HttpResponse =
      POST(s"$customerPath/$customerId/blacklist", payload)

    def getAddresses(customerId: Int): HttpResponse =
      GET(s"$customerPath/$customerId/addresses")

    def getAddress(customerId: Int, addressId: Int): HttpResponse =
      GET(s"$customerPath/$customerId/addresses/$addressId")

    def getCart(customerId: Int): HttpResponse =
      GET(s"$customerPath/$customerId/cart")

    object payment {
      private val paymentPath = "payment-methods"

      object creditCard {
        private val ccPath = "credit-cards"

        def getAll(customerId: Int): HttpResponse =
          GET(s"$customerPath/$customerId/$paymentPath/$ccPath")

        def toggleDefault(customerId: Int,
                          creditCardId: Int,
                          payload: ToggleDefaultCreditCard): HttpResponse =
          POST(s"$customerPath/$customerId/$paymentPath/$ccPath/$creditCardId/default", payload)

        def edit(customerId: Int, creditCardId: Int, payload: EditCreditCard): HttpResponse =
          PATCH(s"$customerPath/$customerId/$paymentPath/$ccPath/$creditCardId", payload)

        def delete(customerId: Int, creditCardId: Int): HttpResponse =
          DELETE(s"$customerPath/$customerId/$paymentPath/$ccPath/$creditCardId")
      }

      object storeCredit {
        private val storeCreditsPath = "store-credit"

        def getTotals(customerId: Int): HttpResponse =
          GET(s"$customerPath/$customerId/$paymentPath/$storeCreditsPath/totals")
      }
    }
  }

  object orderAPI {
    val prefix: String = s"$adminPath/orders"
  }

  object cartsAPI {
    // FIXME: replace with /carts upon update
    val prefix: String = s"$adminPath/orders"
  }

  object storeCreditAPI {
    val prefix: String = s"$adminPath/store-credits"
  }
}
