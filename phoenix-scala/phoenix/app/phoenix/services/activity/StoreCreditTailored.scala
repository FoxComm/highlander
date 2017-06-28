package phoenix.services.activity

import phoenix.models.cord.{Cart, Order}
import phoenix.payloads.StoreCreditPayloads.StoreCreditUpdateStateByCsr
import phoenix.responses.StoreCreditResponse
import phoenix.responses.giftcards.GiftCardResponse
import phoenix.responses.users.UserResponse

object StoreCreditTailored {
  case class StoreCreditCreated(admin: UserResponse, user: UserResponse, storeCredit: StoreCreditResponse)
      extends ActivityBase[StoreCreditCreated]

  case class StoreCreditStateChanged(admin: UserResponse,
                                     storeCredit: StoreCreditResponse,
                                     payload: StoreCreditUpdateStateByCsr)
      extends ActivityBase[StoreCreditStateChanged]

  case class StoreCreditConvertedToGiftCard(admin: UserResponse,
                                            giftCard: GiftCardResponse,
                                            storeCredit: StoreCreditResponse)
      extends ActivityBase[StoreCreditConvertedToGiftCard]

  case class StoreCreditAuthorizedFunds(user: UserResponse,
                                        cart: Cart,
                                        storeCreditIds: Seq[Int],
                                        amount: Long)
      extends ActivityBase[StoreCreditAuthorizedFunds]

  case class StoreCreditCapturedFunds(user: UserResponse,
                                      order: Order,
                                      storeCreditIds: Seq[Int],
                                      amount: Long)
      extends ActivityBase[StoreCreditCapturedFunds]
}
