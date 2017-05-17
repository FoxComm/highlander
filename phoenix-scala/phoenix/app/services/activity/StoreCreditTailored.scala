package services.activity

import models.cord.Order
import models.cord.Cart
import payloads.StoreCreditPayloads.StoreCreditUpdateStateByCsr
import responses.{GiftCardResponse, UserResponse, StoreCreditResponse}

object StoreCreditTailored {
  case class StoreCreditCreated(admin: UserResponse.Root,
                                user: UserResponse.Root,
                                storeCredit: StoreCreditResponse.Root)
      extends ActivityBase[StoreCreditCreated]

  case class StoreCreditStateChanged(admin: UserResponse.Root,
                                     storeCredit: StoreCreditResponse.Root,
                                     payload: StoreCreditUpdateStateByCsr)
      extends ActivityBase[StoreCreditStateChanged]

  case class StoreCreditConvertedToGiftCard(admin: UserResponse.Root,
                                            giftCard: GiftCardResponse.Root,
                                            storeCredit: StoreCreditResponse.Root)
      extends ActivityBase[StoreCreditConvertedToGiftCard]

  case class StoreCreditAuthorizedFunds(user: UserResponse.Root,
                                        cart: Cart,
                                        storeCreditIds: Seq[Int],
                                        amount: Int)
      extends ActivityBase[StoreCreditAuthorizedFunds]

  case class StoreCreditCapturedFunds(user: UserResponse.Root,
                                      order: Order,
                                      storeCreditIds: Seq[Int],
                                      amount: Int)
      extends ActivityBase[StoreCreditCapturedFunds]
}
