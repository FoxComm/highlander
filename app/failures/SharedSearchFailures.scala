package failures

object SharedSearchFailures {

  final case class SharedSearchAssociationNotFound(code: String, associateId: Int) extends Failure {
    override def description = s"sharedSearch with code=$code is not associated to storeAdmin with id=$associateId"
  }

  case object SharedSearchInvalidQueryFailure extends Failure {
    override def description = s"Invalid JSON provided for shared search query"
  }

  case object SharedSearchScopeNotFound extends Failure {
    override def description = "SharedSearch requires a valid scope"
  }
}
