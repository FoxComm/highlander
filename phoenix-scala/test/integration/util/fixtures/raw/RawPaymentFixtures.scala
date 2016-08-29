package util.fixtures.raw

import models._
import models.payment.giftcard._
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import services.giftcards.GiftCardService
import util.fixtures.TestFixtureBase

trait RawPaymentFixtures extends TestFixtureBase {

  trait GiftCard_Raw {
    def giftCardBalance: Int
    def storeAdmin: StoreAdmin
    def reason: Reason

    private val payload = GiftCardCreateByCsr(balance = giftCardBalance, reasonId = reason.id)

    private val giftCardResponse = GiftCardService.createByAdmin(storeAdmin, payload).gimme

    def giftCard: GiftCard = _giftCard

    private val _giftCard = GiftCards.mustFindByCode(giftCardResponse.code).gimme
  }
}
