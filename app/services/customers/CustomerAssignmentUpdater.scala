package services.customers

import models.customer._
import models.{NotificationSubscription, StoreAdmin, StoreAdmins}
import payloads.CustomerBulkAssignmentPayload
import responses.{CustomerResponse, TheResponse, BatchMetadata, BatchMetadataSource}
import responses.CustomerResponse.Root
import responses.BatchMetadata.flattenErrors
import services.Util._
import services.{NotificationManager, LogActivity, CustomerAssigneeNotFound, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.aliases._

import models.activity.{Dimension, ActivityContext}

object CustomerAssignmentUpdater {

  def assign(admin: StoreAdmin, customerId: Int, requestedAssigneeIds: Seq[Int])
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[TheResponse[Root]] = (for {

    customer        ← * <~ Customers.mustFindById404(customerId)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    assignees       ← * <~ CustomerAssignments.assigneesFor(customer).result.toXor
    newAssignments  = adminIds.diff(assignees.map(_.id))
      .map(adminId ⇒ CustomerAssignment(customerId = customer.id, assigneeId = adminId))
    _               ← * <~ CustomerAssignments.createAll(newAssignments)
    newCustomer     ← * <~ Customers.refresh(customer).toXor
    response        ← * <~ CustomerResponse.fromCustomer(customer).toXor
    notFoundAdmins  = diffToFailures(requestedAssigneeIds, adminIds, StoreAdmin)
    assignedAdmins  = response.assignees.filter(a ⇒ newAssignments.map(_.assigneeId).contains(a.assignee.id)).map(_.assignee)
    _               ← * <~ LogActivity.assignedToCustomer(admin, customer, assignedAdmins)
    _               ← * <~ NotificationManager.subscribe(adminIds = assignedAdmins.map(_.id),
      dimension = Dimension.customer, reason = NotificationSubscription.Assigned, objectIds = Seq(customerId.toString))
  } yield TheResponse.build(response, errors = notFoundAdmins)).runTxn()

  def unassign(admin: StoreAdmin, customerId: Int, assigneeId: Int)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {

    customer        ← * <~ Customers.mustFindById404(customerId)
    assignee        ← * <~ StoreAdmins.mustFindById404(assigneeId)
    assignment      ← * <~ CustomerAssignments.byAssignee(assignee).one.mustFindOr(CustomerAssigneeNotFound(customerId, assigneeId))
    _               ← * <~ CustomerAssignments.byAssignee(assignee).delete
    response        ← * <~ CustomerResponse.fromCustomer(customer).toXor
    _               ← * <~ LogActivity.unassignedFromCustomer(admin, customer, assignee)
    _               ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assigneeId),
      dimension = Dimension.customer, reason = NotificationSubscription.Assigned, objectIds = Seq(customerId.toString))
  } yield response).runTxn()

  def assignBulk(admin: StoreAdmin, payload: CustomerBulkAssignmentPayload)(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkCustomerUpdateResponse] = (for {

    // TODO: transfer sorting-paging metadata
    customers       ← * <~ Customers.filter(_.id.inSetBind(payload.customerIds)).result.toXor
    assignee        ← * <~ StoreAdmins.mustFindById400(payload.assigneeId)
    newAssignments  = for (c ← customers) yield CustomerAssignment(customerId = c.id, assigneeId = assignee.id)
    _               ← * <~ CustomerAssignments.createAll(newAssignments)
    response        ← * <~ CustomerQueries.findAll
    success         = customers.filter(c ⇒ newAssignments.map(_.customerId).contains(c.id)).map(_.id)
    _               ← * <~ LogActivity.bulkAssignedToCustomers(admin, assignee, success)
    _               ← * <~ NotificationManager.subscribe(adminIds = Seq(assignee.id), dimension = Dimension.customer,
      reason = NotificationSubscription.Assigned, objectIds = customers.map(_.id.toString)).value
    // Prepare batch response
    batchFailures  = diffToBatchErrors(payload.customerIds, customers.map(_.id), Customer)
    batchMetadata  = BatchMetadata(BatchMetadataSource(Customer, success.map(_.toString), batchFailures))
  } yield response.copy(errors = flattenErrors(batchFailures), batch = Some(batchMetadata))).runTxn()

  def unassignBulk(admin: StoreAdmin, payload: CustomerBulkAssignmentPayload)(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkCustomerUpdateResponse] = (for {

    // TODO: transfer sorting-paging metadata
    customers ← * <~ Customers.filter(_.id.inSetBind(payload.customerIds)).result
    assignee  ← * <~ StoreAdmins.mustFindById400(payload.assigneeId)
    _         ← * <~ CustomerAssignments.filter(_.assigneeId === payload.assigneeId)
      .filter(_.customerId.inSetBind(customers.map(_.id))).delete
    response  ← * <~ CustomerQueries.findAll
    notFound  = diffToFlatFailures(payload.customerIds, customers.map(_.id), Customer)
    success   = customers.filter(c ⇒ payload.customerIds.contains(c.id)).map(_.id)
    _         ← * <~ LogActivity.bulkUnassignedFromCustomers(admin, assignee, success)
    _         ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assignee.id), dimension = Dimension.customer,
      reason = NotificationSubscription.Assigned, objectIds = customers.map(_.id.toString)).value
    // Prepare batch response
    batchFailures  = diffToBatchErrors(payload.customerIds, customers.map(_.id), Customer)
    batchMetadata  = BatchMetadata(BatchMetadataSource(Customer, success.map(_.toString), batchFailures))
  } yield response.copy(errors = flattenErrors(batchFailures), batch = Some(batchMetadata))).runTxn()
}
