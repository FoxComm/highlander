package services

import java.time.Instant

import models._
import payloads.RmaCreatePayload
import services.rmas.RmaService
import util.IntegrationTestBase
import utils.Seeds.Factories
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.time._

class RmaServiceTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  val numberOfInserts = 50

  "RmaService" - {
    "doesn't create duplicate IDs during parallel requests" in new Fixture {
      pendinggi
      (1 to numberOfInserts).par.map(_ ⇒ RmaService.createActions(order, storeAdmin, Rma.Standard).run())

      val rmas = Rmas.result.run().futureValue
      val refs = rmas.map(_.refNum)
      //refs.length must === (numberOfInserts + 1)
      refs.distinct must === (refs)
    }
  }

  trait Fixture {
    val (storeAdmin, customer, order, rma) = (for {
      storeAdmin ← StoreAdmins.saveNew(Factories.storeAdmin)
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(
        status = Order.RemorseHold,
        customerId = customer.id,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
      rma ← Rmas.saveNew(Factories.rma.copy(
        referenceNumber = s"${order.refNum}.1",
        orderId = order.id,
        orderRefNum = order.referenceNumber,
        customerId = customer.id))
    } yield (storeAdmin, customer, order, rma)).run().futureValue
  }
}

