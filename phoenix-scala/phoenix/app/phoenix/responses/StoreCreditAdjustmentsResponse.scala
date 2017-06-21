package phoenix.responses

import java.time.Instant

import phoenix.models.payment.InStorePaymentStates
import phoenix.models.payment.storecredit.StoreCreditAdjustment

object StoreCreditAdjustmentsResponse {
  case class Root(id: Int,
                  createdAt: Instant,
                  debit: Long,
                  availableBalance: Long,
                  state: InStorePaymentStates.State,
                  cordRef: Option[String])
      extends ResponseItem

  def build(adj: StoreCreditAdjustment, cordRef: Option[String] = None): Root =
    Root(id = adj.id,
         createdAt = adj.createdAt,
         debit = adj.debit,
         availableBalance = adj.availableBalance,
         state = adj.state,
         cordRef = cordRef)
}
