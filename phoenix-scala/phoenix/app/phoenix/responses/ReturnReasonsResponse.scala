package phoenix.responses

import java.time.Instant

import phoenix.models.returns.Return.{ReturnType, Standard}
import phoenix.models.returns.ReturnReason
import phoenix.models.returns.ReturnReason.{BaseReason, ReasonType}

object ReturnReasonsResponse {

  case class Root(id: Int = 0,
                  name: String,
                  reasonType: ReasonType = BaseReason,
                  rmaType: ReturnType = Standard,
                  createdAt: Instant = Instant.now,
                  deletedAt: Option[Instant] = None)

  def buildResponse(rr: ReturnReason) =
    Root(rr.id, rr.name, rr.reasonType, rr.rmaType, rr.createdAt, rr.deletedAt)
}
