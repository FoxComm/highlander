package responses

import java.time.Instant

import models.StoreCreditAdjustment

object StoreCreditAdjustmentsResponse {
  final case class Root(
    id: Int,
    createdAt: Instant,
    debit: Int,
    availableBalance: Int,
    status: StoreCreditAdjustment.Status,
    orderRef: Option[String]) extends ResponseItem

  def build(adj: StoreCreditAdjustment, orderRef: Option[String] = None): Root = {
    Root(id = adj.id, createdAt = adj.createdAt, debit = adj.debit, availableBalance = adj.availableBalance, status =
      adj.status, orderRef = orderRef)
  }
}

