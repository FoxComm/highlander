package responses

import models.GiftCardAdjustment

object GiftCardAdjustmentsResponse {
  final case class Root(
    id: Int,
    amount: Int,
    availableBalance: Int,
    state: GiftCardAdjustment.State,
    orderRef: Option[String]) extends ResponseItem

  def build(adj: GiftCardAdjustment, orderRef: Option[String] = None): Root = {
    Root(id = adj.id, amount = adj.getAmount, availableBalance = adj.availableBalance, state = adj.state, orderRef = orderRef)
  }
}
