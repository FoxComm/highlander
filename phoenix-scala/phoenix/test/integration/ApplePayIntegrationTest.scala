import cats.implicits._
import core.failures.GeneralFailure
import phoenix.failures.CaptureFailures
import phoenix.failures.OrderFailures.OnlyOneExternalPaymentIsAllowed
import phoenix.models.cord.CordPaymentState.FullCapture
import phoenix.models.payment.ExternalCharge.FailedAuth
import phoenix.models.payment.creditcard.CreditCardCharges
import phoenix.models.shipping._
import phoenix.payloads.CapturePayloads.{Capture, CaptureLineItem, ShippingCost}
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.payloads.CustomerPayloads.CreateCustomerPayload
import phoenix.payloads.LineItemPayloads._
import phoenix.payloads.PaymentPayloads.{CreateApplePayPayment, CreditCardPayment}
import phoenix.payloads.UpdateShippingMethod
import phoenix.responses.cord._
import phoenix.responses.users.CustomerResponse
import phoenix.responses.{CaptureResponse, CreditCardResponse}
import phoenix.utils.seeds.{Factories, ShipmentSeeds}
import slick.jdbc.PostgresProfile.api._
import testutils.apis._
import testutils.fixtures.PaymentFixtures.CreditCardsFixture
import testutils.fixtures.api._
import testutils._
import utils.MockedApis

class ApplePayIntegrationTest
    extends IntegrationTestBase
    with PhoenixStorefrontApi
    with ApiFixtures
    with ApiFixtureHelpers
    with MockedApis
    with DefaultJwtAdminAuth {

  "POST v1/my/payment-methods/apple-pay" - {
    "Apple pay checkout with funds authorized" in new ApplePayFixture {

      val payment = CreateApplePayPayment(stripeToken = apToken)

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        storefrontPaymentsApi.applePay.create(payment).mustBeOk()
      }

      val skuInCart = cartsApi(refNum).checkout().as[OrderResponse].lineItems.skus.onlyElement
      skuInCart.sku must === (skuCode)
      skuInCart.quantity must === (1)

    }
  }

  "One click apple pay checkout should work" in new ApplePayFixture {
    val payment = CreateApplePayPayment(stripeToken = apToken)

    withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
      storefrontCartsApi.applePayCheckout(payment).as[OrderResponse].referenceNumber must === (
        cart.referenceNumber)
    }
  }

  "Fails with wrongly formatted token" in new ApplePayFixture {
    val payment = CreateApplePayPayment(stripeToken = "random token")

    val api = cartsApi(api_newCustomerCart(customer.id).referenceNumber)
    api.payments.applePay
      .add(payment)(defaultAdminAuth)
      .mustFailWith400(GeneralFailure("Stripe token should start with 'tok_'"))
  }

  "Capture of Apple Pay payments" - {
    "Should capture cc payments if cc payment was authorized" in new ApplePayFixture with CreditCardsFixture {
      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        val cc = storefrontPaymentsApi.creditCards.create(ccPayload).as[CreditCardResponse]
        cartsApi(refNum).payments.creditCard.add(CreditCardPayment(cc.id)).mustBeOk()

        val orderResponse = cartsApi(refNum).checkout().as[OrderResponse]
        val skuInCart     = orderResponse.lineItems.skus

        val capturePayload =
          Capture(orderResponse.referenceNumber,
                  skuInCart.map(sku ⇒ CaptureLineItem(sku.referenceNumbers.head, sku.sku)),
                  ShippingCost(400, "USD"))

        captureApi.capture(capturePayload).mustBeOk()
        ordersApi(refNum).get().asTheResult[OrderResponse].paymentState must === (FullCapture)
      }
    }

    "Fail if order is not in Auth state" in new ApplePayFixture with CreditCardsFixture {
      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        val cc = storefrontPaymentsApi.creditCards.create(ccPayload).as[CreditCardResponse]
        cartsApi(refNum).payments.creditCard.add(CreditCardPayment(cc.id)).mustBeOk()
        val skuInCart = cartsApi(refNum).checkout().as[OrderResponse].lineItems.skus
        CreditCardCharges.filter(_.creditCardId === cc.id).map(_.state).update(FailedAuth).gimme

        val capturePayload =
          Capture(refNum,
                  skuInCart.map(sku ⇒ CaptureLineItem(sku.referenceNumbers.head, sku.sku)),
                  ShippingCost(400, "USD"))

        captureApi
          .capture(capturePayload)
          .mustFailWith400(CaptureFailures.OrderMustBeInAuthState(refNum))
      }
    }

    "Only one external payment should be found" in new ApplePayFixture with CreditCardsFixture {
      val payment = CreateApplePayPayment(stripeToken = apToken)

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        // adding both apple pay and CC payment
        storefrontPaymentsApi.applePay.create(payment).mustBeOk()
        val cc = storefrontPaymentsApi.creditCards.create(ccPayload).as[CreditCardResponse]
        cartsApi(refNum).payments.creditCard.add(CreditCardPayment(cc.id)).mustBeOk()

        // additional check to make sure only one failure has arrived
        cartsApi(refNum).checkout().errors.onlyElement must === (OnlyOneExternalPaymentIsAllowed.description)
        cartsApi(refNum).checkout().mustFailWith400(OnlyOneExternalPaymentIsAllowed)
      }
    }

    "Capture authorized Apple Pay payments" in new ApplePayFixture {
      val payment = CreateApplePayPayment(stripeToken = apToken)

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        val orderResponse = storefrontCartsApi.applePayCheckout(payment).as[OrderResponse]
        val skuInCart     = orderResponse.lineItems.skus

        val capturePayload =
          Capture(orderResponse.referenceNumber,
                  skuInCart.map(sku ⇒ CaptureLineItem(sku.referenceNumbers.head, sku.sku)),
                  ShippingCost(400, "USD"))

        val captureResponse = captureApi.capture(capturePayload).as[CaptureResponse]

        captureResponse.order must === (orderResponse.referenceNumber)
        captureResponse.captured must === (orderResponse.totals.total)
      }
    }
  }

  trait ApplePayFixture extends ShipmentSeeds {
    val apToken           = "tok_1A9YBQJVm1XvTUrO3V8caBvF"
    val customerLoginData = TestLoginData(email = "test@bar.com", password = "pwd")
    val customer = customersApi
      .create(
        CreateCustomerPayload(email = customerLoginData.email,
                              name = faker.Name.name.some,
                              password = customerLoginData.password.some))
      .as[CustomerResponse]

    val cart = cartsApi.create(CreateCart(customerId = customer.id.some)).as[CartResponse]

    val refNum = cart.referenceNumber

    // we don't have shipping method API creation as of PR #910
    val shippingMethod: ShippingMethod = ShippingMethods
      .create(
        Factories.shippingMethods.head.copy(conditions = lowConditions.some,
                                            adminDisplayName = ShippingMethod.expressShippingNameForAdmin))
      .gimme

    cartsApi(refNum).shippingAddress.create(randomAddress()).mustBeOk()

    val skuCode = ProductSku_ApiFixture().skuCode

    val lineItemsPayloads = List(UpdateLineItemsPayload(skuCode, 1))
    cartsApi(refNum).lineItems.add(lineItemsPayloads).mustBeOk()

    cartsApi(refNum).shippingMethod
      .update(UpdateShippingMethod(shippingMethod.id))
      .asTheResult[CartResponse]

  }
}
