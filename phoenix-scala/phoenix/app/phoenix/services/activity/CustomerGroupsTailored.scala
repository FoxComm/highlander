package phoenix.services.activity

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import phoenix.models.account.User
import phoenix.models.customer.CustomerGroup
import phoenix.models.customer.CustomerGroup.GroupType
import phoenix.utils.aliases.Json

object CustomerGroupsTailored {

  case class CustomerGroupActivity(id: Int = 0,
                                   scope: LTree,
                                   createdBy: Int,
                                   name: String,
                                   customersCount: Int = 0,
                                   groupType: GroupType,
                                   clientState: Json,
                                   elasticRequest: Json,
                                   updatedAt: Instant = Instant.now,
                                   createdAt: Instant = Instant.now,
                                   deletedAt: Option[Instant] = None)

  object CustomerGroupActivity {
    def apply(group: CustomerGroup): CustomerGroupActivity =
      CustomerGroupActivity(
        id = group.id,
        scope = group.scope,
        createdBy = group.createdBy,
        name = group.name,
        customersCount = group.customersCount,
        groupType = group.groupType,
        clientState = group.clientState,
        elasticRequest = group.elasticRequest,
        updatedAt = group.updatedAt,
        createdAt = group.createdAt,
        deletedAt = group.deletedAt
      )
  }

  case class CustomerGroupCreated(customerGroup: CustomerGroupActivity, admin: User)
      extends ActivityBase[CustomerGroupCreated]

  case class CustomerGroupUpdated(customerGroup: CustomerGroupActivity, admin: User)
      extends ActivityBase[CustomerGroupUpdated]

  case class CustomerGroupArchived(customerGroup: CustomerGroupActivity, admin: User)
      extends ActivityBase[CustomerGroupArchived]

}
