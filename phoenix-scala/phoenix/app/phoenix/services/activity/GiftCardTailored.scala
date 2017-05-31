package phoenix.services.activity

import phoenix.models.cord.Order
import phoenix.models.cord.Cart
import phoenix.payloads.GiftCardPayloads.GiftCardUpdateStateByCsr
import phoenix.responses.{GiftCardResponse, StoreCreditResponse, UserResponse}

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
                                     amount: Long)
      extends ActivityBase[GiftCardAuthorizedFunds]

  case class GiftCardCapturedFunds(user: UserResponse.Root,
                                   order: Order,
                                   giftCardCodes: Seq[String],
                                   amount: Long)
      extends ActivityBase[GiftCardCapturedFunds]
}
