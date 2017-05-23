package phoenix.services.activity

import phoenix.models.sharedsearch.SharedSearch
import phoenix.responses.UserResponse

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
