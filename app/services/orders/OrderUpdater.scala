package services.orders

import models.Orders
import responses.FullOrder
import services.Result
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.time._

import scala.concurrent.ExecutionContext

object OrderUpdater {

  def increaseRemorsePeriod(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = (for {

    order     ← * <~ Orders.mustFindByRefNum(refNum)
    isRemorse ← * <~ order.mustBeRemorseHold
    updated   ← * <~ Orders.update(order, order.copy(remorsePeriodEnd = order.remorsePeriodEnd.map(_.plusMinutes(15))))
    response  ← * <~ FullOrder.fromOrder(updated).toXor
  } yield response).runT()
}
