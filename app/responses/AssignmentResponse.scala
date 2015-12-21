package responses

import java.time.Instant

import models.{RmaAssignment, StoreAdmin, OrderAssignment}

object AssignmentResponse {

  final case class Root(
    assignee: StoreAdminResponse.Root,
    createdAt: Instant
  ) extends ResponseItem

  def build(assignment: OrderAssignment, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), assignment.createdAt)

  def buildForRma(assignment: RmaAssignment, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), assignment.createdAt)
}
