package phoenix.responses

import java.time.Instant

import phoenix.models.returns.Return.{ReturnType, Standard}
import phoenix.models.returns.ReturnReason
import phoenix.models.returns.ReturnReason.{BaseReason, ReasonType}

case class ReturnReasonsResponse(id: Int,
                                 name: String,
                                 reasonType: ReasonType = BaseReason,
                                 rmaType: ReturnType = Standard,
                                 createdAt: Instant = Instant.now,
                                 deletedAt: Option[Instant] = None)

object ReturnReasonsResponse {

  def build(rr: ReturnReason) =
    ReturnReasonsResponse(rr.id, rr.name, rr.reasonType, rr.rmaType, rr.createdAt, rr.deletedAt)
}
