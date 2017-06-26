package phoenix.services.activity

import phoenix.responses.CatalogResponse
import phoenix.responses.users.UserResponse

object CatalogTailored {

  case class CatalogCreated(admin: UserResponse, catalog: CatalogResponse.Root)
      extends ActivityBase[CatalogCreated]

  case class CatalogUpdated(admin: UserResponse, catalog: CatalogResponse.Root)
      extends ActivityBase[CatalogUpdated]

  case class ProductsAddedToCatalog(admin: UserResponse, catalog: CatalogResponse.Root, productIds: Seq[Int])
      extends ActivityBase[ProductsAddedToCatalog]

  case class ProductRemovedFromCatalog(admin: UserResponse, catalogId: Int, productId: Int)
      extends ActivityBase[ProductRemovedFromCatalog]

}
