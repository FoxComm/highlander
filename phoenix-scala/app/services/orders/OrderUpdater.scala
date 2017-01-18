package services.orders

import models.account._
import models.cord.Orders
import responses.cord.OrderResponse
import services.LogActivity
import utils.aliases._
import utils.db._
import utils.time._

object OrderUpdater {

  def increaseRemorsePeriod(refNum: String, admin: User)(implicit ec: EC,
                                                         db: DB,
                                                         ac: AC): DbResultT[OrderResponse] =
    for {
      order     ← * <~ Orders.mustFindByRefNum(refNum)
      isRemorse ← * <~ order.mustBeRemorseHold
      updated ← * <~ Orders.update(
        order,
        order.copy(remorsePeriodEnd = order.remorsePeriodEnd.map(_.plusMinutes(15))))
      response ← * <~ OrderResponse.fromOrder(updated, grouped = true)
      _        ← * <~ LogActivity.orderRemorsePeriodIncreased(admin, response, order.remorsePeriodEnd)
    } yield response
}
