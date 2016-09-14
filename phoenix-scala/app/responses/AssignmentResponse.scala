package responses

import java.time.Instant
import models.Assignment
import models.account.User
import responses.UserResponse.{build ⇒ buildUser}

object AssignmentResponse {

  case class Root(assignee: UserResponse.Root,
                  assignmentType: Assignment.AssignmentType,
                  createdAt: Instant)
      extends ResponseItem

  def build(assignment: Assignment, admin: User): Root = {

    Root(assignee = buildUser(admin),
         assignmentType = assignment.assignmentType,
         createdAt = assignment.createdAt)
  }
}
