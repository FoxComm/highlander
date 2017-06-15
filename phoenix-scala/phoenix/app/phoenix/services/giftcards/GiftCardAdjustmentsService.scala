package phoenix.services.giftcards

import phoenix.models.cord.{Carts, OrderPayments}
import phoenix.models.payment.giftcard.{GiftCardAdjustments, GiftCards}
import phoenix.responses.GiftCardAdjustmentsResponse._
import slick.jdbc.PostgresProfile.api._
import phoenix.utils.aliases._
import core.db._

object GiftCardAdjustmentsService {

  def forGiftCard(code: String)(implicit ec: EC, db: DB): DbResultT[Seq[Root]] = {

    def maybePaymentQ =
      for {
        pay   ← OrderPayments
        order ← Carts.filter(_.referenceNumber === pay.cordRef)
      } yield (pay, order.referenceNumber)

    def adjustmentQ(giftCardId: Int) = GiftCardAdjustments.filter(_.giftCardId === giftCardId)

    def joinedQ(giftCardId: Int) =
      adjustmentQ(giftCardId).joinLeft(maybePaymentQ).on {
        case (adj, (pay, _)) ⇒ adj.orderPaymentId === pay.id
      }

    for {
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      records  ← * <~ joinedQ(giftCard.id).result
    } yield
      records.map {
        case (adj, Some((_, cordRef))) ⇒
          build(adj, Some(cordRef))
        case (adj, _) ⇒
          build(adj)
      }
  }
}
