package failures

object ImageFailures {

  object ImageNotFoundForContext {
    def apply(imageId: Int, contextId: Int) =
      NotFoundFailure404(s"Image with id=$imageId with context=$contextId cannot be found")
  }

  object AlbumNotFoundForContext {
    def apply(albumId: Int, contextId: Int) =
      NotFoundFailure404(s"Album with id=$albumId with context=$contextId cannot be found")
  }

  case object ImageNotFoundInPayload extends Failure {
    override def description = "Image not found in payload"
  }

  case object ImageFilenameNotFoundInPayload extends Failure {
    override def description = "Image filename not found in payload"
  }

  case object ErrorReceivingImage extends Failure {
    override def description = "Error reading uploaded image from payload"
  }
}
