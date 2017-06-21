package phoenix.failures

import core.failures._

object Util {

  /* Diff lists of model identifiers to produce a list of failures for absent models */
  def diffToFailures[A, B](requested: Seq[A], available: Seq[A], modelType: B): Option[Failures] =
    Failures(requested.diff(available).map(NotFoundFailure404(modelType, _)): _*)

  /* Diff lists of model identifiers to produce a list of warnings for absent models */
  def diffToFlatFailures[A, B](requested: Seq[A], available: Seq[A], modelType: B): Option[List[String]] =
    diffToFailures(requested, available, modelType).map(_.flatten)
}
