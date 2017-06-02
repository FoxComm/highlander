package objectframework.failures

import core.failures.{Failure, NotFoundFailure404}

case object CorruptedContentObject extends Failure {
  override def description = "Unable to parse corrupted content object"
}

case class RelatedContentDoesNotExist(kind: String, commitId: Int) extends Failure {
  override def description = s"Unable to create relation $kind $commitId - object not found"
}

case class ImportCycleFound(entityId: Int, entityKind: String, relCommitId: Int, relKind: String)
    extends Failure {
  override def description =
    s"Unable to create relation from $entityKind $entityId to $relKind $relCommitId - results in relations cycle"
}
