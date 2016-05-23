package services.activity

import models.order.Order
import payloads.GiftCardPayloads.GiftCardUpdateStateByCsr
import responses.{CustomerResponse, GiftCardResponse, StoreAdminResponse, StoreCreditResponse}

object GiftCardTailored {
  case class GiftCardCreated(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.Root)
    extends ActivityBase[GiftCardCreated]

  case class GiftCardStateChanged(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.Root,
    payload: GiftCardUpdateStateByCsr)
    extends ActivityBase[GiftCardStateChanged]

  case class GiftCardConvertedToStoreCredit(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.Root,
    storeCredit: StoreCreditResponse.Root)
    extends ActivityBase[GiftCardConvertedToStoreCredit]

  case class GiftCardAuthorizedFunds(customer: CustomerResponse.Root, order: Order, giftCardCodes: Seq[String],
    amount: Int)
    extends ActivityBase[GiftCardAuthorizedFunds]

  case class GiftCardCapturedFunds(customer: CustomerResponse.Root, order: Order, giftCardCodes: Seq[String],
    amount: Int)
    extends ActivityBase[GiftCardCapturedFunds]
}
