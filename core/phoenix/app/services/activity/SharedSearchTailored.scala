package services.activity

import models.sharedsearch.SharedSearch
import responses.UserResponse

object SharedSearchTailored {
  case class AssociatedWithSearch(admin: UserResponse.Root,
                                  search: SharedSearch,
                                  associates: Seq[UserResponse.Root])
      extends ActivityBase[AssociatedWithSearch]

  case class UnassociatedFromSearch(admin: UserResponse.Root,
                                    search: SharedSearch,
                                    associate: UserResponse.Root)
      extends ActivityBase[UnassociatedFromSearch]
}
