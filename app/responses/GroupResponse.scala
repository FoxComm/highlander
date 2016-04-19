package responses

import java.time.Instant

import models.customer.CustomerDynamicGroup
import org.json4s.JsonAST.{JValue ⇒ Json}

object DynamicGroupResponse {
  case class Root(
    id: Int = 0,
    name: String,
    `type`: String = "dynamic",
    clientState: Json,
    elasticRequest: Json,
    customersCount: Option[Int] = None,
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
