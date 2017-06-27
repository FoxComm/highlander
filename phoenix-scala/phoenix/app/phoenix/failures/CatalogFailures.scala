package phoenix.failures

import core.failures.NotFoundFailure404

object CatalogFailures {
  object CatalogNotFound {
    def apply(id: Int) =
      NotFoundFailure404(s"Catalog $id not found")
  }

  object ProductNotFoundInCatalog {
    def apply(catalogId: Int, productId: Int) =
      NotFoundFailure404(s"Product $productId not found in catalog $catalogId")
  }
}
