package services.activity

import models.Note
import models.returns.Return
import responses.GiftCardResponse
import responses.ReturnResponse.{Root ⇒ ReturnResponse}
import responses.UserResponse.{Root ⇒ UserResponse}

/**
  * Created by aafa on 16.03.17.
  */
object ReturnTailored {

  case class ReturnCreated(admin: Option[UserResponse], rma: ReturnResponse)
      extends ActivityBase[ReturnCreated]

  case class ReturnStateChanged(admin: UserResponse, rma: ReturnResponse, oldState: Return.State)
      extends ActivityBase[ReturnStateChanged]

  /* Return Line Items */
  case class ReturnLineItemsAddedGiftCard(admin: UserResponse,
                                          rma: ReturnResponse,
                                          giftCard: GiftCardResponse.Root)
      extends ActivityBase[ReturnLineItemsAddedGiftCard]

  case class ReturnLineItemsUpdatedGiftCard(admin: UserResponse,
                                            rma: ReturnResponse,
                                            giftCard: GiftCardResponse.Root)
      extends ActivityBase[ReturnLineItemsUpdatedGiftCard]

  case class ReturnLineItemsDeletedGiftCard(admin: UserResponse,
                                            rma: ReturnResponse,
                                            giftCard: GiftCardResponse.Root)
      extends ActivityBase[ReturnLineItemsDeletedGiftCard]

  case class ReturnLineItemsUpdatedQuantities(rma: ReturnResponse,
                                              oldQuantities: Map[String, Int],
                                              newQuantities: Map[String, Int],
                                              admin: Option[UserResponse])
      extends ActivityBase[ReturnLineItemsUpdatedQuantities]


}
