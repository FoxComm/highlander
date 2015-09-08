package utils

import cats.data.Xor
import models.{Order, Orders}
import services.{GeneralFailure, NotFoundFailure, Failures, Result}
import slick.driver.PostgresDriver
import util.{CatsHelpers, IntegrationTestBase}
import utils.Seeds.Factories
import utils.Slick.DbResult
import scala.concurrent.ExecutionContext.Implicits.global

class TableQueryIntegrationTest extends IntegrationTestBase with CatsHelpers {

  "with lock" - {
    "should return locked error" in new Fixture {
      val order = db.run(Orders.save(Factories.order.copy(locked = true))).futureValue

      val result = Orders.findByRefNum(order.referenceNumber).findOneAndRun { order ⇒
        DbResult.dbio(Orders.save(order.copy(status = Order.FraudHold)))
      }.futureValue

      result.isLeft mustBe true
      leftValue(result).head must ===(GeneralFailure("Model is locked"))
    }
  }

  trait Fixture {

  }

}
