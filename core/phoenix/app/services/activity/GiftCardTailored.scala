package services.activity

import models.cord.Order
import models.cord.Cart
import payloads.GiftCardPayloads.GiftCardUpdateStateByCsr
import responses.{GiftCardResponse, UserResponse, StoreCreditResponse}

object GiftCardTailored {
  case class GiftCardCreated(admin: UserResponse.Root, giftCard: GiftCardResponse.Root)
      extends ActivityBase[GiftCardCreated]

  case class GiftCardStateChanged(admin: UserResponse.Root,
                                  giftCard: GiftCardResponse.Root,
                                  payload: GiftCardUpdateStateByCsr)
      extends ActivityBase[GiftCardStateChanged]

  case class GiftCardConvertedToStoreCredit(admin: UserResponse.Root,
                                            giftCard: GiftCardResponse.Root,
                                            storeCredit: StoreCreditResponse.Root)
      extends ActivityBase[GiftCardConvertedToStoreCredit]

  case class GiftCardAuthorizedFunds(user: UserResponse.Root,
                                     cart: Cart,
                                     giftCardCodes: Seq[String],
                                     amount: Int)
      extends ActivityBase[GiftCardAuthorizedFunds]

  case class GiftCardCapturedFunds(user: UserResponse.Root,
                                   order: Order,
                                   giftCardCodes: Seq[String],
                                   amount: Int)
      extends ActivityBase[GiftCardCapturedFunds]
}
