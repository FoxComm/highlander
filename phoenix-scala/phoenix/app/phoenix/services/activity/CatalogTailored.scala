package phoenix.services.activity

import phoenix.responses.{CatalogResponse, UserResponse}

object CatalogTailored {

  case class CatalogCreated(admin: UserResponse.Root, catalog: CatalogResponse.Root)
      extends ActivityBase[CatalogCreated]

  case class CatalogUpdated(admin: UserResponse.Root, catalog: CatalogResponse.Root)
      extends ActivityBase[CatalogUpdated]

}
