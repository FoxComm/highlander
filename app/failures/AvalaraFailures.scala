package failures

object AvalaraFailures {

  case object UnableToMatchResponse extends Failure {
    override def description = "Could not read response from Avalara"
  }

}
