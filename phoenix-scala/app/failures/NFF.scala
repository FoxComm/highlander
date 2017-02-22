package failures

// Not Found Failure
object NFF {

  type FailureParam = (String, Any)

  private def formatFailure(entity: String)(params: FailureParam*): String = {
    val formattedParams: String = params.map {
      case (name, value) ⇒ s"$name=$value"
    }.mkString(", ")

    s"No $entity with $formattedParams found"
  }

  def notFound404(entity: String)(params: FailureParam*): NotFoundFailure404 =
    NotFoundFailure404(formatFailure(entity)(params: _*))

  def notFound400(entity: String)(params: FailureParam*): NotFoundFailure400 =
    NotFoundFailure400(formatFailure(entity)(params: _*))

}
