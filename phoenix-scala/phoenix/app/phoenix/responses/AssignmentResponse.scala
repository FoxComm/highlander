package phoenix.responses

import java.time.Instant

import phoenix.models.Assignment
import phoenix.models.account.User
import phoenix.responses.users.UserResponse

object AssignmentResponse {

  case class Root(assignee: UserResponse, assignmentType: Assignment.AssignmentType, createdAt: Instant)
      extends ResponseItem

  def build(assignment: Assignment, admin: User): Root =
    Root(assignee = UserResponse.build(admin),
         assignmentType = assignment.assignmentType,
         createdAt = assignment.createdAt)
}
