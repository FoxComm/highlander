package failures

import failures.Util._
import models.cord.Order
import utils.friendlyClassName

trait Failure {
  def description: String
}

case class GeneralFailure(a: String) extends Failure {
  override def description = a
}

case class DatabaseFailure(message: String) extends Failure {
  override def description = message
}

case class ElasticsearchFailure(message: String) extends Failure {
  override def description = s"Elasticsearch communication error: $message"
}

case class NotFoundFailure404(message: String) extends Failure {
  override def description = message
}

object NotFoundFailure404 {
  def apply[A](a: A, searchKey: Any): NotFoundFailure404 = {
    NotFoundFailure404(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey not found")
  }

  def apply[A](a: A, searchTerm: String, searchKey: Any): NotFoundFailure404 =
    NotFoundFailure404(s"${friendlyClassName(a)} with $searchTerm=$searchKey not found")

  def apply(className: String, searchTerm: String, searchKey: Any): NotFoundFailure404 = {
    NotFoundFailure404(s"$className with $searchTerm=$searchKey not found")
  }
}

case class NotFoundFailure400(message: String) extends Failure {
  override def description = message
}

object NotFoundFailure400 {
  def apply[A](a: A, searchKey: Any): NotFoundFailure400 = {
    NotFoundFailure400(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey not found")
  }
}

case class StateTransitionNotAllowed(message: String) extends Failure {
  override def description = message
}

object StateTransitionNotAllowed {
  def apply[A](a: A,
               fromState: String,
               toState: String,
               searchKey: Any): StateTransitionNotAllowed = {
    StateTransitionNotAllowed(
        s"Transition from $fromState to $toState is not allowed for ${friendlyClassName(a)} " +
          s"with ${searchTerm(a)}=$searchKey")
  }

  def apply(from: Order.State, to: Order.State, refNum: String): StateTransitionNotAllowed = {
    apply(Order, from.toString, to.toString, refNum)
  }
}

case class NotificationTrailNotFound400(adminId: Int) extends Failure {
  override def description = s"Notification trail for adminId=$adminId not found"
}

case object OpenTransactionsFailure extends Failure {
  override def description = "Open transactions should be canceled/completed"
}

case object EmptyCancellationReasonFailure extends Failure {
  override def description = "Please provide valid cancellation reason"
}

case object InvalidCancellationReasonFailure extends Failure {
  override def description = "Cancellation reason doesn't exist"
}

case class InvalidReasonTypeFailure(name: String) extends Failure {
  override def description = s"Reason type named '$name' doesn't exist"
}

case class InvalidFieldFailure(name: String) extends Failure {
  override def description = s"Invalid value for field '$name' provided"
}

case class AlreadySavedForLater(accountId: Int, skuId: Int) extends Failure {
  override def description =
    s"Customer with id=$accountId already has SKU with id=$skuId saved for later"
}

case class ShipmentNotFoundFailure(cordRefNum: String) extends Failure {
  override def description = s"No shipments found for cart/order with refNum=$cordRefNum"
}
