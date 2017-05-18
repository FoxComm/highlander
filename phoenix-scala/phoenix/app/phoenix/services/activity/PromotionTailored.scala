package phoenix.services.activity

import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses.UserResponse

object PromotionTailored {
  case class PromotionCreated(promotion: PromotionResponse.Root, admin: Option[UserResponse.Root])
      extends ActivityBase[PromotionCreated]

  case class PromotionUpdated(promotion: PromotionResponse.Root, admin: Option[UserResponse.Root])
      extends ActivityBase[PromotionUpdated]
}
