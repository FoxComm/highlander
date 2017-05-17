package phoenix.services.orders

import java.time.Instant

import phoenix.models.cord.{Order, Orders}
import phoenix.utils.aliases._
import utils.db._

object TimeMachine {

  def changePlacedAt(refNum: String, placedAt: Instant)(implicit ec: EC,
                                                        db: DB): DbResultT[Order] =
    for {
      order   ← * <~ Orders.mustFindByRefNum(refNum)
      updated ← * <~ Orders.update(order, order.copy(placedAt = placedAt))
    } yield updated
}
