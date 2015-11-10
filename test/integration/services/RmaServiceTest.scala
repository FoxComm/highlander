package services

import java.time.Instant

import models._
import payloads.RmaCreatePayload
import services.rmas.RmaService
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._
import utils.time._

class RmaServiceTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  "RmaService" - {
    "doesn't create duplicate IDs during parallel requests" in new Fixture {
      def payload = RmaCreatePayload(orderRefNum = order.refNum, rmaType = Rma.Standard)
      val responses = Seq(1, 10).par.map(_ ⇒ RmaService.createByAdmin(storeAdmin, payload))

      val uniqueRefs = Rmas.groupBy(_.referenceNumber).length.result.run().futureValue
      uniqueRefs must === (11)
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

