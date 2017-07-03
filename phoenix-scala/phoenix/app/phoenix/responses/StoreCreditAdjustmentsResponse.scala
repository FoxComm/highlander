package phoenix.responses

import java.time.Instant

import phoenix.models.payment.InStorePaymentStates
import phoenix.models.payment.storecredit.StoreCreditAdjustment

case class StoreCreditAdjustmentsResponse(id: Int,
                                          createdAt: Instant,
                                          debit: Long,
                                          availableBalance: Long,
                                          state: InStorePaymentStates.State,
                                          cordRef: Option[String])
    extends ResponseItem

object StoreCreditAdjustmentsResponse {

  def build(adj: StoreCreditAdjustment, cordRef: Option[String] = None): StoreCreditAdjustmentsResponse =
    StoreCreditAdjustmentsResponse(id = adj.id,
                                   createdAt = adj.createdAt,
                                   debit = adj.debit,
                                   availableBalance = adj.availableBalance,
                                   state = adj.state,
                                   cordRef = cordRef)
}
