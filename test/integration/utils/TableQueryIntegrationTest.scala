package utils

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Order, Orders}
import services.{NotFoundFailure, GeneralFailure}
import util.{CatsHelpers, IntegrationTestBase}
import utils.Seeds.Factories
import utils.Slick.DbResult

class TableQueryIntegrationTest extends IntegrationTestBase with CatsHelpers {

  "for models with id" - {
    "should return model if present" in {
      val order = db.run(Orders.save(Factories.order)).futureValue

      val result = Orders.findByRefNum(order.referenceNumber).findOneAndRun(DbResult.good).futureValue
      result.isRight mustBe true
      rightValue(result) must === (order)
    }

    "should return failure if absent" in {
      val result = Orders.findByRefNum("foobar").findOneAndRun(DbResult.good).futureValue
      result.isLeft mustBe true
      leftValue(result).head must === (NotFoundFailure("Not found"))
    }
  }

  "for models with lock" - {
    "should return locked error" in {
      val order = db.run(Orders.save(Factories.order.copy(locked = true))).futureValue

      val result = Orders.findByRefNum(order.referenceNumber).findOneAndRun { order ⇒
        DbResult.dbio(Orders.save(order.copy(status = Order.FraudHold)))
      }.futureValue

      result.isLeft mustBe true
      leftValue(result).head must === (GeneralFailure("Model is locked"))
    }
  }
}
