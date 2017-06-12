package phoenix.failures

import core.failures.NotFoundFailure404

object CatalogFailures {
  object CatalogNotFound {
    def apply(id: Int) =
      NotFoundFailure404(s"Catalog $id not found")
  }
}
