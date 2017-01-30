package responses

import java.time.Instant

import models.customer.CustomerGroup
import models.customer.CustomerGroup._
import utils.aliases._

object GroupResponses {

  object GroupResponse {

    case class Root(
        id: Int = 0,
        name: String,
        `type`: GroupType = Dynamic,
        clientState: Json,
        elasticRequest: Json,
        customersCount: Int = 0,
        updatedAt: Instant,
        createdAt: Instant
    ) extends ResponseItem

    def build(group: CustomerGroup): Root =
      Root(id = group.id,
           name = group.name,
           `type` = group.groupType,
           clientState = group.clientState,
           elasticRequest = group.elasticRequest,
           customersCount = group.customersCount,
           updatedAt = group.updatedAt,
           createdAt = group.createdAt)
  }

  object CustomerGroupResponse {
    case class Root(id: Int = 0, name: String, `type`: GroupType = Dynamic)

    def build(group: CustomerGroup): Root =
      Root(id = group.id, name = group.name, `type` = group.groupType)
  }

}
