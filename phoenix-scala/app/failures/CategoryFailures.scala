package failures

object CategoryFailures {

  object CategoryNotFoundAtCommit {
    def apply(id: Int, commit: Int) =
      NotFoundFailure404(s"Category $id not found at commit $commit")
  }

  object CategoryNotFoundForContext {
    def apply(categoryId: Int, categoryContextId: Int) =
      NotFoundFailure404(
        s"Category with id=$categoryId with category context $categoryContextId cannot be found")
  }

  object CategoryFormNotFound {
    def apply(id: Int) = NotFoundFailure404("Category form", "id", id)
  }
}
