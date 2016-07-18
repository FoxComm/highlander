package models.traits

import cats.data.Xor
import cats.data.Xor._
import failures.Failures
import failures.LockFailures._

trait Lockable[A] { self: A ⇒

  def isLocked: Boolean

  def primarySearchKey: String

  def mustBeLocked: Failures Xor A =
    if (isLocked) right(this) else left(NotLockedFailure(this, primarySearchKey).single)

  def mustNotBeLocked: Failures Xor A =
    if (!isLocked) right(this) else left(LockedFailure(this, primarySearchKey).single)
}
