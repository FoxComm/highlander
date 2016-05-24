package services.activity

import models.sharedsearch.SharedSearch
import responses.StoreAdminResponse

object SharedSearchTailored {
  case class AssociatedWithSearch(admin: StoreAdminResponse.Root,
                                  search: SharedSearch,
                                  associates: Seq[StoreAdminResponse.Root])
      extends ActivityBase[AssociatedWithSearch]

  case class UnassociatedFromSearch(
      admin: StoreAdminResponse.Root, search: SharedSearch, associate: StoreAdminResponse.Root)
      extends ActivityBase[UnassociatedFromSearch]
}
