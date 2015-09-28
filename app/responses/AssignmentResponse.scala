package responses

import java.time.Instant

import models.{StoreAdmin, OrderAssignment}

object AssignmentResponse {

  final case class Root(
    assignee: StoreAdminResponse.Root,
    assignedAt: Instant
    )

  def build(assignment: OrderAssignment, admin: StoreAdmin): Root = {
    Root(StoreAdminResponse.build(admin), assignment.assignedAt)
  }
}
