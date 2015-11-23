package services.orders

import scala.concurrent.ExecutionContext

import models._
import responses.FullOrder
import services._
import Helpers._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Slick.DbResult
import utils.Slick.UpdateReturning._
import utils.time._

import utils.DbResultT._
import utils.DbResultT.implicits._

object OrderUpdater {

  def increaseRemorsePeriod(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = (for {

    order     ← * <~ mustFindOrderByRefNum(refNum)
    isRemorse ← * <~ order.mustBeRemorseHold
    updated   ← * <~ Orders.update(order, order.copy(remorsePeriodEnd = order.remorsePeriodEnd.map(_.plusMinutes(15))))
    response  ← * <~ FullOrder.fromOrder(updated).toXor
  } yield response).runT()
}
