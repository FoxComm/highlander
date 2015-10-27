package utils

import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.Xor
import models.{Order, Orders}
import services.{LockedFailure, Failures, NotFoundFailure404, GeneralFailure}
import util.{CatsHelpers, IntegrationTestBase}
import utils.Seeds.Factories
import utils.Slick.DbResult
import utils.Slick.UpdateReturning._

class TableQueryIntegrationTest extends IntegrationTestBase with CatsHelpers {

  "for models with id" - {
    "should return model if present" in {
      val order = db.run(Orders.save(Factories.order)).futureValue

      val result = Orders.findByRefNum(order.referenceNumber).selectOneForUpdate(DbResult.good).futureValue
      result.isRight mustBe true
      rightValue(result) must === (order)
    }

    "should return failure if absent" in {
      val result = Orders.findByRefNum("foobar").selectOneForUpdate(DbResult.good).futureValue

      result.isLeft mustBe true
      leftValue(result).head must === (NotFoundFailure404(Order, "foobar"))
    }
  }

  "for models with lock" - {
    "should return locked error" in new Fixture {
      val result = finder.selectOneForUpdate { _ ⇒
        DbResult.fromDbio(finder.map(_.status).updateReturning(Orders.map(identity), Order.FraudHold).head)
      }.futureValue

      leftValue(result).head mustBe LockedFailure(Order, order.referenceNumber)
    }

    "should bypass lock" in new Fixture {
      val result = finder.selectOne ({ _ ⇒
        DbResult.fromDbio(finder.map(_.status).updateReturning(Orders.map(identity), Order.FraudHold).head)
      }, checks = Set.empty).futureValue

      rightValue(result).status mustBe Order.FraudHold
    }

    trait Fixture {
      val order = db.run(Orders.save(Factories.order.copy(locked = true))).futureValue
      val finder = Orders.findByRefNum(order.referenceNumber)
    }
  }
}
