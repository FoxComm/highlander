package failures

// Not Found Failure
object NFF {

  type FailureParams = Map[String, Any]

  private def formatFailure(entity: String)(params: FailureParams): String = {
    val formattedParams: String = params.map {
      case (name, value) ⇒ s"$name=$value"
    }.mkString(", ")

    s"No $entity with $formattedParams found"
  }

  def notFound404(entity: String)(params: FailureParams): NotFoundFailure404 =
    NotFoundFailure404(formatFailure(entity)(params))

  def notFound400(entity: String)(params: FailureParams): NotFoundFailure400 =
    NotFoundFailure400(formatFailure(entity)(params))

}
