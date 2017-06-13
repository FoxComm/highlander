package phoenix.services.activity

import phoenix.responses.{CatalogResponse, UserResponse}

object CatalogTailored {

  case class CatalogCreated(admin: UserResponse.Root, catalog: CatalogResponse.Root)
      extends ActivityBase[CatalogCreated]

  case class CatalogUpdated(admin: UserResponse.Root, catalog: CatalogResponse.Root)
      extends ActivityBase[CatalogUpdated]

  case class ProductsAddedToCatalog(admin: UserResponse.Root,
                                    catalog: CatalogResponse.Root,
                                    productIds: Seq[Int])
      extends ActivityBase[ProductsAddedToCatalog]

  case class ProductRemovedFromCatalog(admin: UserResponse.Root, catalogId: Int, productId: Int)
      extends ActivityBase[ProductRemovedFromCatalog]

}
