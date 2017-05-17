package responses

import java.time.Instant

import models.returns.Return.{ReturnType, Standard}
import models.returns.ReturnReason
import models.returns.ReturnReason.{BaseReason, ReasonType}
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse.{Root ⇒ User}

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
