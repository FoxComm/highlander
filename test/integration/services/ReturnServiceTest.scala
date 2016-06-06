package services

import java.time.Instant

import models.customer.Customers
import models.order.{Orders, Order}
import models.returns._
import models.StoreAdmins
import payloads.ReturnPayloads.ReturnCreatePayload
import services.returns.ReturnService
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._
import utils.seeds.Seeds.Factories
import utils.time._

import scala.concurrent.Future

class ReturnServiceTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  val numberOfInserts = 20

  "ReturnService" - {
    "doesn't create duplicate IDs during parallel requests for single order" in new Fixture {
      val payload = ReturnCreatePayload(order.refNum, Return.Standard)
      val futures = (1 to numberOfInserts).map { _ ⇒
        ReturnService.createByAdmin(admin, payload)
      }
      Future.sequence(futures).futureValue

      val returns = Returns.result.run().futureValue
      val refs    = returns.map(_.refNum)
      refs.length must ===(numberOfInserts)
      refs.distinct must ===(refs)

      val orderUpdated = Orders.findOneById(order.id).run().futureValue.value
      orderUpdated.returnCount must ===(numberOfInserts)

      val rmaCount = Returns.findByOrderRefNum(order.refNum).length.result.run().futureValue
      rmaCount must ===(numberOfInserts)
    }
  }

  trait Fixture {
    val (admin, order) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      order ← * <~ Orders.create(
                 Factories.order.copy(referenceNumber = "ABC-123",
                                      state = Order.RemorseHold,
                                      customerId = customer.id,
                                      remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
    } yield (admin, order)).runTxn().futureValue.rightVal
  }
}
