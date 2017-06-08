package phoenix.services.orders

import phoenix.models.account._
import phoenix.models.cord.Orders
import phoenix.responses.cord.OrderResponse
import phoenix.services.LogActivity
import phoenix.utils.aliases._
import phoenix.utils.time._
import core.db._

object OrderUpdater {

  def increaseRemorsePeriod(refNum: String,
                            admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[OrderResponse] =
    for {
      order     ← * <~ Orders.mustFindByRefNum(refNum)
      isRemorse ← * <~ order.mustBeRemorseHold
      updated ← * <~ Orders.update(
                 order,
                 order.copy(remorsePeriodEnd = order.remorsePeriodEnd.map(_.plusMinutes(15))))
      response ← * <~ OrderResponse.fromOrder(updated, grouped = true)
      _        ← * <~ LogActivity().orderRemorsePeriodIncreased(admin, response, order.remorsePeriodEnd)
    } yield response
}
