package models

import models.cord.OrderPayments
import models.payment.storecredit._
import testutils._
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class StoreCreditIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with TestObjectContext {

  "StoreCreditTest" - {
    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new Fixture {
      storeCredit.originalBalance must === (5000)
      storeCredit.currentBalance must === (5000)
      storeCredit.availableBalance must === (5000)
    }

    "updates availableBalance if auth adjustment is created + cancel handling" in new Fixture {
      val adjustment = StoreCredits.auth(storeCredit, Some(payment.id), 1000).gimme

      val updatedStoreCredit = StoreCredits.findOneById(storeCredit.id).run().futureValue.value
      updatedStoreCredit.availableBalance must === (storeCredit.availableBalance - 1000)

      StoreCreditAdjustments.cancel(adjustment.id).run().futureValue
      val canceledStoreCredit = StoreCredits.findOneById(storeCredit.id).run().futureValue.value
      canceledStoreCredit.availableBalance must === (storeCredit.availableBalance)
    }

    "updates availableBalance and currentBalance if capture adjustment is created + cancel handling" in new Fixture {
      val adjustment = StoreCredits.capture(storeCredit, Some(payment.id), 1000).gimme

      val updatedStoreCredit = StoreCredits.findOneById(storeCredit.id).run().futureValue.value
      updatedStoreCredit.availableBalance must === (storeCredit.availableBalance - 1000)
      updatedStoreCredit.currentBalance must === (storeCredit.currentBalance - 1000)

      StoreCreditAdjustments.cancel(adjustment.id).run().futureValue
      val canceledStoreCredit = StoreCredits.findOneById(storeCredit.id).run().futureValue.value
      canceledStoreCredit.availableBalance must === (storeCredit.availableBalance)
      canceledStoreCredit.currentBalance must === (storeCredit.currentBalance)
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed {
    val (origin, storeCredit, payment) = (for {
      reason ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
      origin ← * <~ StoreCreditManuals.create(
        StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      sc ← * <~ StoreCredits.create(
        Factories.storeCredit.copy(accountId = customer.accountId, originId = origin.id))
      sCredit ← * <~ StoreCredits.findOneById(sc.id)
      payment ← * <~ OrderPayments.create(
        Factories.storeCreditPayment
          .copy(cordRef = cart.refNum, paymentMethodId = sc.id, amount = Some(25)))
    } yield (origin, sCredit.value, payment)).gimme
  }
}
