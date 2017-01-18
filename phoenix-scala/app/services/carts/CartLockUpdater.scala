package services.carts

import models.account._
import models.cord.{CartLockEvent, CartLockEvents, Carts}
import responses.cord.CartResponse
import utils.aliases._
import utils.db._

object CartLockUpdater {

  def lock(refNum: String,
           admin: User)(implicit ec: EC, db: DB, ctx: OC): DbResultT[CartResponse] =
    for {
      cart ← * <~ Carts.mustFindByRefNum(refNum)
      _    ← * <~ cart.mustNotBeLocked
      _    ← * <~ Carts.update(cart, cart.copy(isLocked = true))
      _ ← * <~ CartLockEvents.create(
        CartLockEvent(cartRef = cart.refNum, lockedBy = admin.accountId))
      resp ← * <~ CartResponse.buildRefreshed(cart)
    } yield resp

  def unlock(refNum: String)(implicit ec: EC, db: DB, ctx: OC): DbResultT[CartResponse] =
    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ cart.mustBeLocked
      _        ← * <~ Carts.update(cart, cart.copy(isLocked = false))
      response ← * <~ CartResponse.buildRefreshed(cart)
    } yield response
}
