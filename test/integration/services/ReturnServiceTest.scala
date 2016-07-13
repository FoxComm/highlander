package services

import scala.concurrent.ExecutionContext.Implicits.global

import models.StoreAdmins
import models.cord._
import models.customer.Customers
import models.returns._
import payloads.ReturnPayloads.ReturnCreatePayload
import services.returns.ReturnService
import util.{IntegrationTestBase, TestObjectContext}
import utils.db._
import utils.seeds.Seeds.Factories

class ReturnServiceTest extends IntegrationTestBase with TestObjectContext {

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

      val rmaCount = Returns.findByOrderRefNum(order.refNum).length.gimme
      rmaCount must === (numberOfInserts)
    }
  }

  trait Fixture {
    val (admin, order) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      cart     ← * <~ Carts.create(Factories.cart.copy(customerId = customer.id))
      order    ← * <~ Orders.create(cart.toOrder())
    } yield (admin, order)).gimme
  }
}
