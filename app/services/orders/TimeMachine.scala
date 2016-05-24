package services.orders

import java.time.Instant

import models.order._
import services.Result
import utils.aliases._
import utils.db.DbResultT._

object TimeMachine {

  def changePlacedAt(refNum: String, placedAt: Instant)(implicit ec: EC, db: DB): Result[Order] =
    (for {
      order   ← * <~ Orders.mustFindByRefNum(refNum)
      updated ← * <~ Orders.update(order, order.copy(placedAt = Some(placedAt)))
    } yield updated).runTxn()
}
