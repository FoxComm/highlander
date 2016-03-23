package failures

import models.order.Order
import utils.friendlyClassName
import Util._

trait Failure {
  def description: String
}

final case class GeneralFailure(a: String) extends Failure {
  override def description = a
}

final case class DatabaseFailure(message: String) extends Failure {
  override def description = message
}

final case class NotFoundFailure404(message: String) extends Failure {
  override def description = message
}

object NotFoundFailure404 {
  def apply[A](a: A, searchKey: Any): NotFoundFailure404 = {
    NotFoundFailure404(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey not found")
  }
}

final case class NotFoundFailure400(message: String) extends Failure {
  override def description = message
}

object NotFoundFailure400 {
  def apply[A](a: A, searchKey: Any): NotFoundFailure400 = {
    NotFoundFailure400(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey not found")
  }
}

final case class StateTransitionNotAllowed(message: String) extends Failure {
  override def description = message
}

object StateTransitionNotAllowed {
  def apply[A](a: A, fromState: String, toState: String, searchKey: Any): StateTransitionNotAllowed = {
    StateTransitionNotAllowed(s"Transition from $fromState to $toState is not allowed for ${friendlyClassName(a)} " +
      s"with ${searchTerm(a)}=$searchKey")
  }

  def apply(from: Order.State, to: Order.State, refNum: String): StateTransitionNotAllowed = {
    apply(Order, from.toString, to.toString, refNum)
  }
}

final case class NotificationTrailNotFound400(adminId: Int) extends Failure {
  override def description = s"Notification trail for adminId=$adminId not found"
}

case object OpenTransactionsFailure extends Failure {
  override def description = "Open transactions should be canceled/completed"
}

object AssigneeNotFound {
  def apply[A](a: A, searchKey: Any, assigneeId: Int): NotFoundFailure400 = {
    NotFoundFailure400(s"storeAdmin with id=$assigneeId is not assigned to ${friendlyClassName(a)} " +
      s"with ${searchTerm(a)}=$searchKey")
  }
}

case object EmptyCancellationReasonFailure extends Failure {
  override def description = "Please provide valid cancellation reason"
}


case object InvalidCancellationReasonFailure extends Failure {
  override def description = "Cancellation reason doesn't exist"
}

final case class InvalidReasonTypeFailure(name: String) extends Failure {
  override def description = s"Reason type named '$name' doesn't exist"
}

final case class InvalidFieldFailure(name: String) extends Failure {
  override def description = s"Invalid value for field '$name' provided"
}

case object LoginFailed extends Failure {
  override def description = s"Email or password invalid"
}

final case class AlreadySavedForLater(customerId: Int, skuId: Int) extends Failure {
  override def description = s"Customer with id=$customerId already has SKU with id=$skuId saved for later"
}

final case class ShipmentNotFoundFailure(orderRefNum: String) extends Failure {
  override def description = s"No shipments found for order with refNum=$orderRefNum"
}

final case class AlreadyAssignedFailure(message: String) extends Failure {
  override def description = message
}

object AlreadyAssignedFailure {
  def apply[A](a: A, searchKey: Any, storeAdminId: Int): AlreadyAssignedFailure = {
    val msg = s"storeAdmin with id=$storeAdminId is already assigned to ${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey"
    AlreadyAssignedFailure(msg)
  }
}
