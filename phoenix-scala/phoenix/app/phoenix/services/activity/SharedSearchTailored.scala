package phoenix.services.activity

import phoenix.models.sharedsearch.SharedSearch
import phoenix.responses.users.UserResponse

object SharedSearchTailored {
  case class AssociatedWithSearch(admin: UserResponse, search: SharedSearch, associates: Seq[UserResponse])
      extends ActivityBase[AssociatedWithSearch]

  case class UnassociatedFromSearch(admin: UserResponse, search: SharedSearch, associate: UserResponse)
      extends ActivityBase[UnassociatedFromSearch]
}
