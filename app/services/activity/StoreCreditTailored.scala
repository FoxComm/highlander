package services.activity

import models.order.Order
import payloads.StoreCreditPayloads.StoreCreditUpdateStateByCsr
import responses.{CustomerResponse, GiftCardResponse, StoreAdminResponse, StoreCreditResponse}

object StoreCreditTailored {
  case class StoreCreditCreated(admin: StoreAdminResponse.Root,
                                customer: CustomerResponse.Root,
                                storeCredit: StoreCreditResponse.Root)
      extends ActivityBase[StoreCreditCreated]

  case class StoreCreditStateChanged(admin: StoreAdminResponse.Root,
                                     storeCredit: StoreCreditResponse.Root,
                                     payload: StoreCreditUpdateStateByCsr)
      extends ActivityBase[StoreCreditStateChanged]

  case class StoreCreditConvertedToGiftCard(admin: StoreAdminResponse.Root,
                                            giftCard: GiftCardResponse.Root,
                                            storeCredit: StoreCreditResponse.Root)
      extends ActivityBase[StoreCreditConvertedToGiftCard]

  case class StoreCreditAuthorizedFunds(customer: CustomerResponse.Root,
                                        order: Order,
                                        storeCreditIds: Seq[Int],
                                        amount: Int)
      extends ActivityBase[StoreCreditAuthorizedFunds]

  case class StoreCreditCapturedFunds(customer: CustomerResponse.Root,
                                      order: Order,
                                      storeCreditIds: Seq[Int],
                                      amount: Int)
      extends ActivityBase[StoreCreditCapturedFunds]
}
