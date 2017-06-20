package phoenix.services.orders

import java.time.Instant

import core.db._
import phoenix.models.cord.{Order, Orders}

object TimeMachine {

  def changePlacedAt(refNum: String, placedAt: Instant)(implicit ec: EC, db: DB): DbResultT[Order] =
    for {
      order   ← * <~ Orders.mustFindByRefNum(refNum)
      updated ← * <~ Orders.update(order, order.copy(placedAt = placedAt))
    } yield updated
}
