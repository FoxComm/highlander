package phoenix.failures

import core.failures.Failure
import core.utils.friendlyClassName
import phoenix.models.cord.Order

case class ElasticsearchFailure(message: String) extends Failure {
  override def description = s"Elasticsearch communication error: $message"
}

case class StateTransitionNotAllowed(message: String) extends Failure {
  override def description = message
}

object StateTransitionNotAllowed {
  def apply[A](a: A, fromState: String, toState: String, searchKey: Any): StateTransitionNotAllowed =
    StateTransitionNotAllowed(
      s"Transition from $fromState to $toState is not allowed for ${friendlyClassName(a)} " +
        s"with key=$searchKey")

  def apply(from: Order.State, to: Order.State, refNum: String): StateTransitionNotAllowed =
    apply(Order, from.toString, to.toString, refNum)
}

case class NotificationTrailNotFound400(adminId: Int) extends Failure {
  override def description = s"Notification trail for adminId=$adminId not found"
}

case class LastSeenNotFound400(adminId: Int) extends Failure {
  override def description = s"Notification Last Seen not found for adminId=$adminId"
}

case object OpenTransactionsFailure extends Failure {
  override def description = "Open transactions should be canceled/completed"
}

case object EmptyCancellationReasonFailure extends Failure {
  override def description = "Please provide valid cancellation reason"
}

case object NonEmptyCancellationReasonFailure extends Failure {
  override def description = "Cancellation reason shouldn't be specified in irrelevant context"
}

case object InvalidCancellationReasonFailure extends Failure {
  override def description = "Invalid cancellation reason provided"
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
