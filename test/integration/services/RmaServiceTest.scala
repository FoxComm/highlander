package services

import java.time.Instant

import scala.concurrent.Future

import models._
import services.rmas.RmaService
import util.IntegrationTestBase
import utils.Seeds.Factories
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.time._

class RmaServiceTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  val numberOfInserts = 100

  "RmaService" - {
    "doesn't create duplicate IDs during parallel requests for single order" in new Fixture {
      val futures = (1 to numberOfInserts).map { _ ⇒ RmaService.createActions(order, admin, Rma.Standard).run() }
      Future.sequence(futures).futureValue

      val rmas = Rmas.result.run().futureValue
      val refs = rmas.map(_.refNum)
      refs.length must === (numberOfInserts)
      refs.distinct must === (refs)

      val orderUpdated = Orders.findOneById(order.id).run().futureValue.value
      orderUpdated.rmaCount must === (numberOfInserts)

      val rmaCount = Rmas.findByOrderRefNum(order.refNum).length.result.run().futureValue
      rmaCount must === (numberOfInserts)
    }
  }

  trait Fixture {
    val (admin, order) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      order    ← * <~ Orders.create(Factories.order.copy(
        referenceNumber = "ABC-123",
        status = Order.RemorseHold,
        customerId = customer.id,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
    } yield (admin, order)).runT().futureValue.rightVal
  }
}

