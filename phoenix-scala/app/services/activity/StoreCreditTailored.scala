package services.activity

import models.cord.Order
import models.cord.Cart
import payloads.StoreCreditPayloads.StoreCreditUpdateStateByCsr
import responses.{GiftCardResponse, UserResponse, StoreCreditResponse}

object StoreCreditTailored {
  case class StoreCreditCreated(admin: UserResponse.Root,
                                customer: UserResponse.Root,
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

  case class StoreCreditAuthorizedFunds(customer: UserResponse.Root,
                                        cart: Cart,
                                        storeCreditIds: Seq[Int],
                                        amount: Int)
      extends ActivityBase[StoreCreditAuthorizedFunds]

  case class StoreCreditCapturedFunds(customer: UserResponse.Root,
                                      order: Order,
                                      storeCreditIds: Seq[Int],
                                      amount: Int)
      extends ActivityBase[StoreCreditCapturedFunds]
}
