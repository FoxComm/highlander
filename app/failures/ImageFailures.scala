package failures

object ImageFailures {

  object ImageNotFoundForContext {
    def apply(imageId: Int, contextId: Int) =
      NotFoundFailure404(s"Image with id=$imageId with context=$contextId cannot be found")
  }

  object ImageFormNotFound {
    def apply(formId: Int) =
      NotFoundFailure404(s"Image form with id=$formId cannot be found")
  }

  object ImageShadowNotFound {
    def apply(shadowId: Int) =
      NotFoundFailure404(s"Image shadow with id=$shadowId cannot be found")
  }

  object AlbumNotFoundForContext {
    def apply(albumId: Int, contextId: Int) =
      NotFoundFailure404(s"Album with id=$albumId with context=$contextId cannot be found")
  }

}
