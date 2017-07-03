package phoenix.services.activity

import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses.users.UserResponse

object PromotionTailored {
  case class PromotionCreated(promotion: PromotionResponse, admin: Option[UserResponse])
      extends ActivityBase[PromotionCreated]

  case class PromotionUpdated(promotion: PromotionResponse, admin: Option[UserResponse])
      extends ActivityBase[PromotionUpdated]
}
