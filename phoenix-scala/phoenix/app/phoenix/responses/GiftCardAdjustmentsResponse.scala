package phoenix.responses

import phoenix.models.payment.InStorePaymentStates
import phoenix.models.payment.giftcard.GiftCardAdjustment

object GiftCardAdjustmentsResponse {
  case class Root(id: Int,
                  amount: Long,
                  availableBalance: Long,
                  state: InStorePaymentStates.State,
                  cordRef: Option[String])
      extends ResponseItem

  def build(adj: GiftCardAdjustment, cordRef: Option[String] = None): Root =
    Root(id = adj.id,
         amount = adj.getAmount,
         availableBalance = adj.availableBalance,
         state = adj.state,
         cordRef = cordRef)
}
