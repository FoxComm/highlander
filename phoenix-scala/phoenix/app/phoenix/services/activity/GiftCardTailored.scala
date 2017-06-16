package phoenix.services.activity

import phoenix.models.cord.{Cart, Order}
import phoenix.payloads.GiftCardPayloads.GiftCardUpdateStateByCsr
import phoenix.responses.users.UserResponse
import phoenix.responses.{GiftCardResponse, StoreCreditResponse}

object GiftCardTailored {
  case class GiftCardCreated(admin: UserResponse, giftCard: GiftCardResponse.Root)
      extends ActivityBase[GiftCardCreated]

  case class GiftCardStateChanged(admin: UserResponse,
                                  giftCard: GiftCardResponse.Root,
                                  payload: GiftCardUpdateStateByCsr)
      extends ActivityBase[GiftCardStateChanged]

  case class GiftCardConvertedToStoreCredit(admin: UserResponse,
                                            giftCard: GiftCardResponse.Root,
                                            storeCredit: StoreCreditResponse.Root)
      extends ActivityBase[GiftCardConvertedToStoreCredit]

  case class GiftCardAuthorizedFunds(user: UserResponse, cart: Cart, giftCardCodes: Seq[String], amount: Long)
      extends ActivityBase[GiftCardAuthorizedFunds]

  case class GiftCardCapturedFunds(user: UserResponse, order: Order, giftCardCodes: Seq[String], amount: Long)
      extends ActivityBase[GiftCardCapturedFunds]
}
