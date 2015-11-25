package utils

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Order, Orders}
import services.{LockedFailure, NotFoundFailure404}
import util.{CatsHelpers, IntegrationTestBase}
import utils.Seeds.Factories
import utils.Slick.DbResult
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

class TableQueryIntegrationTest extends IntegrationTestBase with CatsHelpers {

  "for models with id" - {
    "should return model if present" in {
      val order = Orders.create(Factories.order).run().futureValue.rightVal

      val result = Orders.findByRefNum(order.referenceNumber).selectOneForUpdate(DbResult.good).futureValue
      result.isRight mustBe true
      rightValue(result) must === (order)
    }

    "should return failure if absent" in {
      val result = Orders.findByRefNum("foobar").selectOneForUpdate(DbResult.good).futureValue

      result.leftVal must === (NotFoundFailure404(Order, "foobar").single)
    }
  }

  "for models with lock" - {
    "should return locked error" in new Fixture {
      val result = finder.selectOneForUpdate { _ ⇒
        finder.map(_.status).updateReturningHead(Orders.map(identity), Order.FraudHold)
      }.futureValue.leftVal

      result.head mustBe LockedFailure(Order, order.referenceNumber)
    }

    "should bypass lock" in new Fixture {
      val result = finder.selectOne ({ _ ⇒
        finder.map(_.status).updateReturningHead(Orders.map(identity), Order.FraudHold)
      }, checks = Set.empty).futureValue.rightVal

      result.status mustBe Order.FraudHold
    }

    trait Fixture {
      val order = Orders.create(Factories.order.copy(isLocked = true)).run().futureValue.rightVal
      val finder = Orders.findByRefNum(order.referenceNumber)
    }
  }
}
