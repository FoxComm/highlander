package services

import models.cord._
import models.returns._
import payloads.ReturnPayloads.ReturnCreatePayload
import services.returns.ReturnService
import util.Fixtures.{EmptyCustomerCartFixture, StoreAdminFixture}
import util.{IntegrationTestBase, TestObjectContext}
import utils.db._

class ReturnServiceTest extends IntegrationTestBase with TestObjectContext {

  val numberOfInserts = 20

  "ReturnService" - {
    "doesn't create duplicate IDs during parallel requests for single order" in new Fixture {
      val payload = ReturnCreatePayload(order.refNum, Return.Standard)
      val futures = (1 to numberOfInserts).map { _ ⇒
        ReturnService.createByAdmin(storeAdmin, payload)
      }
      DbResultT.sequence(futures).gimme

      val refs = Returns.gimme.map(_.refNum)
      refs.length must === (numberOfInserts)
      refs.distinct must === (refs)

      val rmaCount = Returns.findByOrderRefNum(order.refNum).length.gimme
      rmaCount must === (numberOfInserts)
    }
  }

  trait Fixture extends EmptyCustomerCartFixture with StoreAdminFixture {
    val order = Orders.create(cart.toOrder()).gimme
  }
}
