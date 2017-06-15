package phoenix.services

import core.db._
import phoenix.failures.GiftCardFailures.GiftCardConvertFailure
import phoenix.failures.OpenTransactionsFailure
import phoenix.failures.StoreCreditFailures.StoreCreditConvertFailure
import phoenix.models.account._
import phoenix.models.payment.giftcard._
import phoenix.models.payment.storecredit._
import phoenix.responses.users.UserResponse
import phoenix.responses.{GiftCardResponse, StoreCreditResponse}
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object CustomerCreditConverter {

  def toStoreCredit(giftCardCode: String,
                    accountId: Int,
                    admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreCreditResponse.Root] =
    for {
      giftCard ← * <~ GiftCards.mustFindByCode(giftCardCode)
      _        ← * <~ failIf(!giftCard.isActive, GiftCardConvertFailure(giftCard))
      _        ← * <~ Users.mustFindByAccountId(accountId)
      _ ← * <~ GiftCardAdjustments
           .lastAuthByGiftCardId(giftCard.id)
           .mustNotFindOneOr(OpenTransactionsFailure)
      // Update state and make adjustment
      _ ← * <~ GiftCards
           .findActiveByCode(giftCard.code)
           .map(_.state)
           .update(GiftCard.FullyRedeemed)
      _ ← * <~ GiftCards.redeemToStoreCredit(giftCard, admin)

      // Finally, convert to Store Credit
      conversion ← * <~ StoreCreditFromGiftCards.create(StoreCreditFromGiftCard(giftCardId = giftCard.id))
      storeCredit ← * <~ StoreCredits.create(
                     StoreCredit(
                       accountId = accountId,
                       originId = conversion.id,
                       scope = giftCard.scope,
                       originType = StoreCredit.GiftCardTransfer,
                       currency = giftCard.currency,
                       originalBalance = giftCard.currentBalance,
                       currentBalance = giftCard.currentBalance
                     ))

      // Activity
      _ ← * <~ LogActivity().gcConvertedToSc(admin, giftCard, storeCredit)
    } yield StoreCreditResponse.build(storeCredit)

  def toGiftCard(storeCreditId: Int,
                 accountId: Int,
                 admin: User)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[GiftCardResponse.Root] =
    for {
      credit ← * <~ StoreCredits.mustFindById404(storeCreditId)
      _      ← * <~ failIf(!credit.isActive, StoreCreditConvertFailure(credit))
      _      ← * <~ Users.mustFindByAccountId(accountId)
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
      conversion ← * <~ GiftCardFromStoreCredits.create(GiftCardFromStoreCredit(storeCreditId = credit.id))
      giftCard ← * <~ GiftCards.create(
                  GiftCard(
                    scope = Scope.current,
                    originId = conversion.id,
                    originType = GiftCard.FromStoreCredit,
                    currency = credit.currency,
                    originalBalance = credit.currentBalance,
                    currentBalance = credit.currentBalance
                  ))

      // Activity
      _ ← * <~ LogActivity().scConvertedToGc(admin, giftCard, credit)
    } yield GiftCardResponse.build(giftCard, None, Some(UserResponse.build(admin)))
}
