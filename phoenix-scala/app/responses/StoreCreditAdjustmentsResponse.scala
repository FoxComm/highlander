package responses

import io.circe.syntax._
import java.time.Instant
import models.payment.InStorePaymentStates
import models.payment.storecredit.StoreCreditAdjustment
import utils.aliases._
import utils.json.codecs._

object StoreCreditAdjustmentsResponse {
  case class Root(id: Int,
                  createdAt: Instant,
                  debit: Int,
                  availableBalance: Int,
                  state: InStorePaymentStates.State,
                  cordRef: Option[String])
      extends ResponseItem {
    def json: Json = this.asJson
  }

  def build(adj: StoreCreditAdjustment, cordRef: Option[String] = None): Root = {
    Root(id = adj.id,
         createdAt = adj.createdAt,
         debit = adj.debit,
         availableBalance = adj.availableBalance,
         state = adj.state,
         cordRef = cordRef)
  }
}
