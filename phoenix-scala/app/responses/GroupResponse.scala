package responses

import java.time.Instant

import models.customer.CustomerDynamicGroup
import utils.aliases._

object DynamicGroupResponses {

  object DynamicGroupResponse {

    case class Root(
        id: Int = 0,
        name: String,
        `type`: String = "dynamic",
        clientState: Json,
        elasticRequest: Json,
        customersCount: Int = 0,
        updatedAt: Instant,
        createdAt: Instant
    ) extends ResponseItem

    def build(group: CustomerDynamicGroup): Root =
      Root(id = group.id,
           name = group.name,
           clientState = group.clientState,
           elasticRequest = group.elasticRequest,
           customersCount = group.customersCount,
           updatedAt = group.updatedAt,
           createdAt = group.createdAt)
  }

  object CustomerGroupResponse {
    case class Root(id: Int = 0, name: String, `type`: String = "dynamic")

    def build(group: CustomerDynamicGroup): Root =
      Root(id = group.id, name = group.name)
  }

}
