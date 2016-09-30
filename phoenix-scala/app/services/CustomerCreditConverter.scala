package services

import failures.GiftCardFailures.GiftCardConvertFailure
import failures.OpenTransactionsFailure
import failures.StoreCreditFailures.StoreCreditConvertFailure
import models.StoreAdmin
import models.customer.Customers
import models.payment.giftcard._
import models.payment.storecredit._
import responses.{GiftCardResponse, StoreAdminResponse, StoreCreditResponse}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CustomerCreditConverter {

  def toStoreCredit(giftCardCode: String, customerId: Int, admin: StoreAdmin)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[StoreCreditResponse.Root] =
    for {

      giftCard ← * <~ GiftCards.mustFindByCode(giftCardCode)
      _        ← * <~ failIf(!giftCard.isActive, GiftCardConvertFailure(giftCard))
      _        ← * <~ Customers.mustFindById404(customerId)
      _ ← * <~ GiftCardAdjustments
           .lastAuthByGiftCardId(giftCard.id)
           .one
           .mustNotFindOr(OpenTransactionsFailure)
      // Update state and make adjustment
      _ ← * <~ GiftCards
           .findActiveByCode(giftCard.code)
           .map(_.state)
           .update(GiftCard.FullyRedeemed)
      adjustment ← * <~ GiftCards.redeemToStoreCredit(giftCard, admin)

      // Finally, convert to Store Credit
      conversion ← * <~ StoreCreditFromGiftCards.create(
                      StoreCreditFromGiftCard(giftCardId = giftCard.id))
      sc = StoreCredit.buildFromGcTransfer(customerId, giftCard).copy(originId = conversion.id)
      storeCredit ← * <~ StoreCredits.create(sc)

      // Activity
      _ ← * <~ LogActivity.gcConvertedToSc(admin, giftCard, storeCredit)
    } yield StoreCreditResponse.build(storeCredit)

  def toGiftCard(storeCreditId: Int, customerId: Int, admin: StoreAdmin)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[GiftCardResponse.Root] =
    for {

      credit ← * <~ StoreCredits.mustFindById404(storeCreditId)
      _      ← * <~ failIf(!credit.isActive, StoreCreditConvertFailure(credit))
      _      ← * <~ Customers.mustFindById404(customerId)
      _ ← * <~ StoreCreditAdjustments
           .lastAuthByStoreCreditId(credit.id)
           .one
           .mustNotFindOr(OpenTransactionsFailure)
      // Update state and make adjustment
      scUpdated ← * <~ StoreCredits
                   .findActiveById(credit.id)
                   .map(_.state)
                   .update(StoreCredit.FullyRedeemed)
      adjustment ← * <~ StoreCredits.redeemToGiftCard(credit, admin)
      // Convert to Gift Card
      conversion ← * <~ GiftCardFromStoreCredits.create(
                      GiftCardFromStoreCredit(storeCreditId = credit.id))
      giftCard ← * <~ GiftCards.create(
                    GiftCard(originId = conversion.id,
                             originType = GiftCard.FromStoreCredit,
                             currency = credit.currency,
                             originalBalance = credit.currentBalance,
                             currentBalance = credit.currentBalance))

      // Activity
      _ ← * <~ LogActivity.scConvertedToGc(admin, giftCard, credit)
    } yield GiftCardResponse.build(giftCard, None, Some(StoreAdminResponse.build(admin)))
}
