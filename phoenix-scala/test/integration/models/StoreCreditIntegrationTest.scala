package models

import models.cord.OrderPayments
import models.payment.storecredit._
import payloads.PaymentPayloads._
import responses.StoreCreditResponse
import testutils._
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._

class StoreCreditIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with ApiFixtures
    with ApiFixtureHelpers
    with TestObjectContext {

  "StoreCreditTest" - {
    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new Fixture {
      storeCreditResponse.originalBalance must === (5000)
      storeCreditResponse.currentBalance must === (5000)
      storeCreditResponse.availableBalance must === (5000)
    }

    // Checkout scenario
    "updates availableBalance if auth adjustment is created + cancel handling" in new Fixture {
      val adjustment = StoreCredits.auth(storeCreditModel, Some(payment.id), 1000).gimme

      val updatedStoreCredit = StoreCredits.findOneById(storeCreditResponse.id).gimme.value
      updatedStoreCredit.availableBalance must === (storeCreditResponse.availableBalance - 1000)

      StoreCreditAdjustments.cancel(adjustment.id).gimme
      val canceledStoreCredit = StoreCredits.findOneById(storeCreditResponse.id).gimme.value
      canceledStoreCredit.availableBalance must === (storeCreditResponse.availableBalance)
    }

    // Shipment scenario
    "updates availableBalance and currentBalance if capture adjustment is created + cancel handling" in new Fixture {
      // Auth must happen before capture!
      StoreCredits.auth(storeCreditModel, Some(payment.id), 1000).gimme
      // And now capture
      val adjustment = StoreCredits.capture(storeCreditModel, Some(payment.id), 1000).gimme

      val updatedStoreCredit = StoreCredits.findOneById(storeCreditResponse.id).gimme.value
      updatedStoreCredit.availableBalance must === (storeCreditResponse.availableBalance - 1000)
      updatedStoreCredit.currentBalance must === (storeCreditResponse.currentBalance - 1000)

      StoreCreditAdjustments.cancel(adjustment.id).gimme
      val canceledStoreCredit = StoreCredits.findOneById(storeCreditResponse.id).gimme.value
      canceledStoreCredit.availableBalance must === (storeCreditResponse.availableBalance)
      canceledStoreCredit.currentBalance must === (storeCreditResponse.currentBalance)
    }
  }

  trait Fixture extends StoreAdmin_Seed with Reason_Baked {

    val customerId = api_newCustomer().id
    val cartRef    = api_newCustomerCart(customerId).referenceNumber

    val storeCreditResponse = customersApi(customerId).payments.storeCredit
      .create(CreateManualStoreCredit(amount = 5000, reasonId = reason.id))
      .as[StoreCreditResponse.Root]

    cartsApi(cartRef).payments.storeCredit.add(StoreCreditPayment(amount = 5000)).mustBeOk()

    val storeCreditModel = StoreCredits.mustFindById400(storeCreditResponse.id).gimme
    val payment          = OrderPayments.findAllByCordRef(cartRef).gimme.headOption.value
  }
}
