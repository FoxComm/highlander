package responses

import java.time.Instant

import models.payment.storecredit.StoreCreditAdjustment

object StoreCreditAdjustmentsResponse {
  case class Root(id: Int,
                  createdAt: Instant,
                  debit: Int,
                  availableBalance: Int,
                  state: StoreCreditAdjustment.State,
                  cordRef: Option[String])
      extends ResponseItem

  def build(adj: StoreCreditAdjustment, cordRef: Option[String] = None): Root = {
    Root(id = adj.id,
         createdAt = adj.createdAt,
         debit = adj.debit,
         availableBalance = adj.availableBalance,
         state = adj.state,
         cordRef = cordRef)
  }
}
