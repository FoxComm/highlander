package services.activity

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import models.account.User
import models.customer.CustomerGroup
import models.customer.CustomerGroup.GroupType

object CustomerGroupsTailored {

  case class CustomerGroupActivity(id: Int = 0,
                                   scope: LTree,
                                   createdBy: Int,
                                   name: String,
                                   customersCount: Int = 0,
                                   groupType: GroupType,
                                   updatedAt: Instant = Instant.now,
                                   createdAt: Instant = Instant.now,
                                   deletedAt: Option[Instant] = None)

  object CustomerGroupActivity {
    def apply(group: CustomerGroup): CustomerGroupActivity = {
      CustomerGroupActivity(id = group.id,
                            scope = group.scope,
                            createdBy = group.createdBy,
                            name = group.name,
                            customersCount = group.customersCount,
                            groupType = group.groupType,
                            updatedAt = group.updatedAt,
                            createdAt = group.createdAt,
                            deletedAt = group.deletedAt)
    }
  }

  case class CustomerGroupCreated(customerGroup: CustomerGroupActivity, admin: User)
      extends ActivityBase[CustomerGroupCreated]

  case class CustomerGroupUpdated(customerGroup: CustomerGroupActivity, admin: User)
      extends ActivityBase[CustomerGroupUpdated]

  case class CustomerGroupArchived(customerGroup: CustomerGroupActivity, admin: User)
      extends ActivityBase[CustomerGroupArchived]

}
