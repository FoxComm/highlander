package services.activity

import models.order.Order
import responses.{CustomerResponse, GiftCardResponse, StoreCreditResponse, StoreAdminResponse}

object StoreCreditTailored {
  final case class StoreCreditCreated(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
    storeCredit: StoreCreditResponse.Root)
    extends ActivityBase[StoreCreditCreated]

  final case class StoreCreditStateChanged(admin: StoreAdminResponse.Root, storeCredit: StoreCreditResponse.Root,
    payload: payloads.StoreCreditUpdateStateByCsr)
    extends ActivityBase[StoreCreditStateChanged]

  final case class StoreCreditConvertedToGiftCard(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.Root,
    storeCredit: StoreCreditResponse.Root)
    extends ActivityBase[StoreCreditConvertedToGiftCard]

  final case class StoreCreditAuthorizedFunds(customer: CustomerResponse.Root, order: Order, storeCreditIds: Seq[Int],
    amount: Int)
    extends ActivityBase[StoreCreditAuthorizedFunds]

  final case class StoreCreditCapturedFunds(customer: CustomerResponse.Root, order: Order, storeCreditIds: Seq[Int],
    amount: Int)
    extends ActivityBase[StoreCreditCapturedFunds]
}
