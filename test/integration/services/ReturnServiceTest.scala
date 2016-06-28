package services

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import models.StoreAdmins
import models.customer.Customers
import models.order.{Order, Orders}
import models.returns._
import payloads.ReturnPayloads.ReturnCreatePayload
import services.returns.ReturnService
import util.IntegrationTestBase
import utils.db.DbResultT
import utils.db.DbResultT._
import utils.seeds.Seeds.Factories
import utils.time._

class ReturnServiceTest extends IntegrationTestBase {

  val numberOfInserts = 20

  "ReturnService" - {
    "doesn't create duplicate IDs during parallel requests for single order" in new Fixture {
      val payload = ReturnCreatePayload(order.refNum, Return.Standard)
      val futures = (1 to numberOfInserts).map { _ ⇒
        ReturnService.createByAdmin(admin, payload)
      }
      DbResultT.sequence(futures).gimme

      val refs = Returns.gimme.map(_.refNum)
      refs.length must === (numberOfInserts)
      refs.distinct must === (refs)

      val orderUpdated = Orders.findOneByRefNum(order.refNum).gimme.value
      orderUpdated.returnCount must === (numberOfInserts)

      val rmaCount = Returns.findByOrderRefNum(order.refNum).length.gimme
      rmaCount must === (numberOfInserts)
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
    } yield (admin, order)).gimme
  }
}
