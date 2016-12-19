package responses

import models.payment.PaymentStates
import models.payment.giftcard.GiftCardAdjustment

object GiftCardAdjustmentsResponse {
  case class Root(id: Int,
                  amount: Int,
                  availableBalance: Int,
                  state: PaymentStates.State,
                  cordRef: Option[String])
      extends ResponseItem

  def build(adj: GiftCardAdjustment, cordRef: Option[String] = None): Root = {
    Root(id = adj.id,
         amount = adj.getAmount,
         availableBalance = adj.availableBalance,
         state = adj.state,
         cordRef = cordRef)
  }
}
