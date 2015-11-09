package responses

import java.time.Instant

import models.{RmaAssignment, StoreAdmin, OrderAssignment}

object AssignmentResponse {

  final case class Root(
    assignee: StoreAdminResponse.Root,
    assignedAt: Instant
    ) extends ResponseItem

  def build(assignment: OrderAssignment, admin: StoreAdmin): Root = {
    Root(StoreAdminResponse.build(admin), assignment.assignedAt)
  }

  def buildForRma(assignment: RmaAssignment, admin: StoreAdmin): Root = {
    Root(StoreAdminResponse.build(admin), assignment.assignedAt)
  }
}
