package models.traits

import cats.data.Xor
import cats.data.Xor._
import failures.Failures
import failures.LockFailures._
import monocle._

trait Lockable[A] { self: A ⇒

  def isLocked: Boolean

  def primarySearchKeyLens: Lens[A, String]

  def mustBeLocked: Failures Xor A =
    if (isLocked) right(this) else left(NotLockedFailure(this, primarySearchKeyLens.get(this)).single)

  def mustNotBeLocked: Failures Xor A =
    if (!isLocked) right(this) else left(LockedFailure(this, primarySearchKeyLens.get(this)).single)

}
