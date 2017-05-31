package phoenix.failures

import core.failures.{Failure, NotFoundFailure404}
import phoenix.models.image.{Album, Image}

object ImageFailures {

  object ImageNotFoundForContext {
    def apply(imageId: Int, contextId: Int) =
      NotFoundFailure404(s"Image with id=$imageId with context=$contextId cannot be found")
  }

  object ImageNotFoundInAlbum {
    def apply(imageId: Image#Id, albumid: Album#Id) =
      NotFoundFailure404(s"Image with id=$imageId not found in album with id=$albumid")
  }

  object AlbumNotFoundForContext {
    def apply(albumId: Int, contextId: Int) =
      NotFoundFailure404(s"Album with id=$albumId with context=$contextId cannot be found")
  }

  object AlbumWithShadowNotFound {
    def apply(shadowId: Int) =
      NotFoundFailure404(s"Album with shadow id=$shadowId cannot be found")
  }

  case object ImageNotFoundInPayload extends Failure {
    override def description = "Image not found in payload"
  }

  case class ImageFetchFailed(httpCode: Int) extends Failure {
    override def description: String = s"Can't fetch image, status code $httpCode"
  }

  case class ImageTemporarySaveFailed(e: Throwable) extends Failure {
    override def description: String = s"Can't save image, internal error: $e"
  }

  case class ImageUploadFailedGeneralFailure(e: Throwable) extends Failure {
    override def description: String = s"Can't upload image, internal error: $e"
  }

  case object ImageFilenameNotFoundInPayload extends Failure {
    override def description = "Image filename not found in payload"
  }

  case object ErrorReceivingImage extends Failure {
    override def description = "Error reading uploaded image from payload"
  }

  case class UnsupportedImageType(contentType: String) extends Failure {
    override def description: String = s"Unsupported image format: $contentType"
  }

  case object UnknownImageType extends Failure {
    override def description: String = "Can't recognize image format"
  }

  case class InvalidImageUrl(url: String) extends Failure {
    override def description: String = s"Invalid image url: $url"
  }
}
