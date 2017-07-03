package phoenix.responses

import java.time.Instant

import phoenix.models.Assignment
import phoenix.models.account.User
import phoenix.responses.users.UserResponse

case class AssignmentResponse(assignee: UserResponse,
                              assignmentType: Assignment.AssignmentType,
                              createdAt: Instant)
    extends ResponseItem

object AssignmentResponse {

  def build(assignment: Assignment, admin: User): AssignmentResponse =
    AssignmentResponse(assignee = UserResponse.build(admin),
                       assignmentType = assignment.assignmentType,
                       createdAt = assignment.createdAt)
}
