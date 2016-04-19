package responses

import java.time.Instant

import models.payment.storecredit.StoreCreditAdjustment

object StoreCreditAdjustmentsResponse {
  case class Root(
    id: Int,
    createdAt: Instant,
    debit: Int,
    availableBalance: Int,
    state: StoreCreditAdjustment.State,
    orderRef: Option[String]) extends ResponseItem

  def build(adj: StoreCreditAdjustment, orderRef: Option[String] = None): Root = {
    Root(id = adj.id, createdAt = adj.createdAt, debit = adj.debit, availableBalance = adj.availableBalance, state =
      adj.state, orderRef = orderRef)
  }
}

