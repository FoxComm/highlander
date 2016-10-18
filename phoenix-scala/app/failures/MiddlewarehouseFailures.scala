package failures

object MiddlewarehouseFailures {

  case class MiddlewarehouseError(message: String) extends Failure {
    override def description = message
  }

  case object UnableToHoldLineItems extends Failure {
    override def description = s"Unable to hold line items"
  }

  case object UnableToCancelHoldLineItems extends Failure {
    override def description = s"Unable to cancel hold on line items"
  }
}
