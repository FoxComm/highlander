package failures

object ConsulFailures {

  case object UnableToWriteToConsul extends Failure {
    override def description = "Could not write to consul"
  }

}
