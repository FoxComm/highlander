package responses

import models.StoreCreditAdjustment

object StoreCreditAdjustmentsResponse {
  final case class Root(
    id: Int,
    debit: Int,
    availableBalance: Int,
    state: StoreCreditAdjustment.Status,
    orderRef: Option[String]) extends ResponseItem

  def build(adj: StoreCreditAdjustment, orderRef: Option[String] = None): Root = {
    Root(id = adj.id, debit = adj.debit, availableBalance = adj.availableBalance, state = adj.status, orderRef = orderRef)
  }
}

