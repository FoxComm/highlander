package testutils.fixtures.raw

import phoenix.models._
import phoenix.models.account._
import phoenix.models.payment.giftcard._
import phoenix.payloads.GiftCardPayloads.GiftCardCreateByCsr
import phoenix.services.giftcards.GiftCardService
import phoenix.utils.aliases._
import testutils.fixtures.TestFixtureBase

trait RawPaymentFixtures extends TestFixtureBase {

  trait GiftCard_Raw {
    implicit def au: AU

    def giftCardBalance: Long
    def storeAdmin: User
    def reason: Reason

    private val payload = GiftCardCreateByCsr(balance = giftCardBalance, reasonId = reason.id)

    private val giftCardResponse = GiftCardService.createByAdmin(storeAdmin, payload).gimme

    def giftCard: GiftCard = _giftCard

    private val _giftCard = GiftCards.mustFindByCode(giftCardResponse.code).gimme
  }
}
