package responses

import java.time.Instant
import models.{Assignment, StoreAdmin}
import responses.StoreAdminResponse.{build ⇒ buildAdmin}

object AssignmentResponse {

  case class Root(assignee: StoreAdminResponse.Root,
                  assignmentType: Assignment.AssignmentType,
                  createdAt: Instant)
      extends ResponseItem

  def build(assignment: Assignment, admin: StoreAdmin): Root =
    Root(assignee = buildAdmin(admin),
         assignmentType = assignment.assignmentType,
         createdAt = assignment.createdAt)
}
