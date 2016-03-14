package services.orders

import models.StoreAdmin
import models.order.Orders
import responses.order.FullOrder
import services.{LogActivity, Result}
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.time._
import utils.aliases._

object OrderUpdater {

  def increaseRemorsePeriod(refNum: String, admin: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[FullOrder.Root] = (for {
    order     ← * <~ Orders.mustFindByRefNum(refNum)
    isRemorse ← * <~ order.mustBeRemorseHold
    updated   ← * <~ Orders.update(order, order.copy(remorsePeriodEnd = order.remorsePeriodEnd.map(_.plusMinutes(15))))
    response  ← * <~ FullOrder.fromOrder(updated).toXor
    _         ← * <~ LogActivity.orderRemorsePeriodIncreased(admin, response, order.remorsePeriodEnd)
  } yield response).runTxn()
}
