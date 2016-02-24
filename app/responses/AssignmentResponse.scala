package responses

import java.time.Instant

import models.customer.CustomerAssignment
import models.order.OrderAssignment
import models.rma.RmaAssignment
import models.payment.giftcard.GiftCardAssignment
import models.StoreAdmin

object AssignmentResponse {

  final case class Root(
    assignee: StoreAdminResponse.Root,
    createdAt: Instant
  ) extends ResponseItem

  def build(assignment: OrderAssignment, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), assignment.createdAt)

  def buildForRma(assignment: RmaAssignment, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), assignment.createdAt)

  def buildForCustomer(assignment: CustomerAssignment, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), assignment.createdAt)

  def buildForGiftCard(assignment: GiftCardAssignment, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), assignment.createdAt)
}
