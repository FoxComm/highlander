package responses

import java.time.Instant
import models.Assignment
import models.admin.StoreAdminUser
import models.account.User
import responses.StoreAdminResponse.{build ⇒ buildAdmin}

object AssignmentResponse {

  case class Root(assignee: StoreAdminResponse.Root,
                  assignmentType: Assignment.AssignmentType,
                  createdAt: Instant)
      extends ResponseItem

  def build(assignment: Assignment, admin: User, storeAdminUser: StoreAdminUser): Root = {

    require(storeAdminUser.accountId == admin.accountId)
    require(storeAdminUser.userId == admin.id)

    Root(assignee = buildAdmin(admin, storeAdminUser),
         assignmentType = assignment.assignmentType,
         createdAt = assignment.createdAt)
  }
}
