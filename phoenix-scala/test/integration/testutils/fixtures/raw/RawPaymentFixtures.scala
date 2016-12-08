package testutils.fixtures.raw

import models._
import models.account._
import models.payment.giftcard._
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import services.giftcards.GiftCardService
import testutils.fixtures.TestFixtureBase
import utils.aliases._

trait RawPaymentFixtures extends TestFixtureBase {

  trait GiftCard_Raw {
    implicit def au: AU

    def giftCardBalance: Int
    def storeAdmin: User
    def reason: Reason

    private val payload = GiftCardCreateByCsr(balance = giftCardBalance, reasonId = reason.id)

    private val giftCardResponse = GiftCardService.createByAdmin(storeAdmin, payload).gimme

    def giftCard: GiftCard = _giftCard

    private val _giftCard = GiftCards.mustFindByCode(giftCardResponse.code).gimme
  }
}
