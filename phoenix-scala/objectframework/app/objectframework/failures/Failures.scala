package objectframework.failures

import core.failures.{Failure, NotFoundFailure404}

case object CorruptedContentObject extends Failure {
  override def description = "Unable to parse corrupted content object"
}

case class RelatedContentDoesNotExist(kind: String, commitId: Int) extends Failure {
  override def description = s"Unable to create relation $kind $commitId - object not found"
}
