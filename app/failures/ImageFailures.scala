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
  
}
