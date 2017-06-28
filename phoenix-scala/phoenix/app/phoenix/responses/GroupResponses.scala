package phoenix.responses

import java.time.Instant

import phoenix.models.customer.CustomerGroup._
import phoenix.models.customer.{CustomerGroup, CustomerGroupTemplate}
import phoenix.utils.aliases._

object GroupResponses {

  case class GroupResponse(
      id: Int,
      name: String,
      groupType: GroupType = Dynamic,
      clientState: Json,
      elasticRequest: Json,
      customersCount: Int = 0,
      updatedAt: Instant,
      createdAt: Instant
  ) extends ResponseItem

  object GroupResponse {

    def build(group: CustomerGroup): GroupResponse =
      GroupResponse(
        id = group.id,
        name = group.name,
        groupType = group.groupType,
        clientState = group.clientState,
        elasticRequest = group.elasticRequest,
        customersCount = group.customersCount,
        updatedAt = group.updatedAt,
        createdAt = group.createdAt
      )
  }

  case class CustomerGroupResponse(id: Int, name: String, groupType: GroupType = Dynamic)

  object CustomerGroupResponse {

    def build(group: CustomerGroup): CustomerGroupResponse =
      CustomerGroupResponse(id = group.id, name = group.name, groupType = group.groupType)
  }

  case class GroupTemplateResponse(id: Int,
                                   name: String,
                                   groupType: GroupType = Dynamic,
                                   clientState: Json,
                                   elasticRequest: Json)

  object GroupTemplateResponse {

    def build(groupTemplate: CustomerGroupTemplate): GroupTemplateResponse =
      GroupTemplateResponse(
        id = groupTemplate.id,
        name = groupTemplate.name,
        groupType = CustomerGroup.Template,
        clientState = groupTemplate.clientState,
        elasticRequest = groupTemplate.elasticRequest
      )
  }

}
