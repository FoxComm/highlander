package models

import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._

class StoreCreditIntegrationTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "StoreCreditTest" - {
    "sets availableBalance and currentBalance equal to originalBalance upon insert" in new Fixture {
      storeCredit.originalBalance must === (5000)
      storeCredit.currentBalance must === (5000)
      storeCredit.availableBalance must === (5000)
    }

    "updates availableBalance if auth adjustment is created + cancel handling" in new Fixture {
      val adjustment = StoreCredits.auth(storeCredit, Some(payment.id), 1000).run().futureValue.rightVal

      val updatedStoreCredit = StoreCredits.findOneById(storeCredit.id).run().futureValue.value
      updatedStoreCredit.availableBalance must === (storeCredit.availableBalance - 1000)

      StoreCreditAdjustments.cancel(adjustment.id).run().futureValue
      val canceledStoreCredit = StoreCredits.findOneById(storeCredit.id).run().futureValue.value
      canceledStoreCredit.availableBalance must === (storeCredit.availableBalance)
    }

    "updates availableBalance and currentBalance if capture adjustment is created + cancel handling" in new Fixture {
      val adjustment = StoreCredits.capture(storeCredit, Some(payment.id), 1000).run().futureValue.rightVal

      val updatedStoreCredit = StoreCredits.findOneById(storeCredit.id).run().futureValue.value
      updatedStoreCredit.availableBalance must === (storeCredit.availableBalance - 1000)
      updatedStoreCredit.currentBalance must === (storeCredit.currentBalance - 1000)

      StoreCreditAdjustments.cancel(adjustment.id).run().futureValue
      val canceledStoreCredit = StoreCredits.findOneById(storeCredit.id).run().futureValue.value
      canceledStoreCredit.availableBalance must === (storeCredit.availableBalance)
      canceledStoreCredit.currentBalance must === (storeCredit.currentBalance)
    }
  }

  trait Fixture {
    val (customer, origin, storeCredit, payment) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      order    ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      origin   ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = admin.id, reasonId = reason.id))
      sc       ← * <~ StoreCredits.create(Factories.storeCredit.copy(customerId = customer.id, originId = origin.id))
      sCredit  ← * <~ StoreCredits.findOneById(sc.id).toXor
      payment  ← * <~ OrderPayments.create(Factories.storeCreditPayment.copy(orderId = order.id,
        paymentMethodId = sc.id, amount = Some(25)))
    } yield (customer, origin, sCredit.value, payment)).runT().futureValue.rightVal
  }
}


