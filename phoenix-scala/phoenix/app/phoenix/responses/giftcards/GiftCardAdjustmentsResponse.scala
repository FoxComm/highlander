package phoenix.responses.giftcards

import phoenix.models.payment.InStorePaymentStates
import phoenix.models.payment.giftcard.GiftCardAdjustment
import phoenix.responses.ResponseItem

case class GiftCardAdjustmentsResponse(id: Int,
                                       amount: Long,
                                       availableBalance: Long,
                                       state: InStorePaymentStates.State,
                                       cordRef: Option[String])
    extends ResponseItem

object GiftCardAdjustmentsResponse {

  def build(adj: GiftCardAdjustment, cordRef: Option[String] = None): GiftCardAdjustmentsResponse =
    GiftCardAdjustmentsResponse(id = adj.id,
                                amount = adj.getAmount,
                                availableBalance = adj.availableBalance,
                                state = adj.state,
                                cordRef = cordRef)
}
