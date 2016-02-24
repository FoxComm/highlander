package services.orders

import models.activity.ActivityContext
import models.StoreAdmin
import models.order.Orders
import responses.order.FullOrder
import services.{LogActivity, Result}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.time._

import scala.concurrent.ExecutionContext

object OrderUpdater {

  def increaseRemorsePeriod(refNum: String, admin: StoreAdmin)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[FullOrder.Root] = (for {
    order     ← * <~ Orders.mustFindByRefNum(refNum)
    isRemorse ← * <~ order.mustBeRemorseHold
    updated   ← * <~ Orders.update(order, order.copy(remorsePeriodEnd = order.remorsePeriodEnd.map(_.plusMinutes(15))))
    response  ← * <~ FullOrder.fromOrder(updated).toXor
    _         ← * <~ LogActivity.orderRemorsePeriodIncreased(admin, response, order.remorsePeriodEnd)
  } yield response).runTxn()
}
