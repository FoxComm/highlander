package phoenix.services.giftcards

import core.db._
import phoenix.models.cord.{Carts, OrderPayments}
import phoenix.models.payment.giftcard.{GiftCardAdjustments, GiftCards}
import phoenix.responses.giftcards.GiftCardAdjustmentsResponse
import slick.jdbc.PostgresProfile.api._

object GiftCardAdjustmentsService {

  def forGiftCard(code: String)(implicit ec: EC, db: DB): DbResultT[Seq[GiftCardAdjustmentsResponse]] = {

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
          GiftCardAdjustmentsResponse.build(adj, Some(cordRef))
        case (adj, _) ⇒
          GiftCardAdjustmentsResponse.build(adj)
      }
  }
}
