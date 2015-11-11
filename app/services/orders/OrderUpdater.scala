package services.orders

import scala.concurrent.ExecutionContext

import models._
import responses.FullOrder
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.DbResult
import utils.Slick.UpdateReturning._
import utils.time._

object OrderUpdater {

  def increaseRemorsePeriod(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOneForUpdate { order ⇒
      order.status match {
        case Order.RemorseHold ⇒
          finder
            .map(_.remorsePeriodEnd)
            .updateReturningHead(Orders.map(identity), order.remorsePeriodEnd.map(_.plusMinutes(15)))
            .flatMap(xor ⇒ xorMapDbio(xor)(FullOrder.fromOrder))

        case _ ⇒ DbResult.failure(GeneralFailure("Order is not in RemorseHold status"))
      }
    }
  }
}
