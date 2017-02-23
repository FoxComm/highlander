package services

import cats.implicits._
import models.returns._
import payloads.ReturnPayloads.ReturnCreatePayload
import services.returns.ReturnService
import testutils._
import testutils.fixtures.BakedFixtures
import utils.db._

class ReturnServiceTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  val numberOfInserts = 20

  "ReturnService" - {
    "doesn't create duplicate IDs during parallel requests for single order" in new Fixture {
      val payload = ReturnCreatePayload(order.refNum, Return.Standard)
      val futures = (1 to numberOfInserts).toList.map { _ ⇒
        ReturnService.createByAdmin(storeAdmin, payload)
      }
      DbResultT.sequenceJoiningFailures(futures).gimme

      val refs = Returns.gimme.map(_.refNum)
      refs.length must === (numberOfInserts)
      refs.distinct must === (refs)

      val rmaCount = Returns.findByOrderRefNum(order.refNum).length.gimme
      rmaCount must === (numberOfInserts)
    }
  }

  trait Fixture extends Order_Baked with StoreAdmin_Seed
}
