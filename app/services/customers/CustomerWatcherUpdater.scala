package services.customers

import models.activity.{Dimension, ActivityContext}
import models.customer._
import models.{NotificationSubscription, StoreAdmin, StoreAdmins}
import payloads.CustomerBulkWatchersPayload
import responses.{CustomerResponse, TheResponse, BatchMetadata, BatchMetadataSource}
import responses.CustomerResponse.Root
import responses.BatchMetadata.flattenErrors
import services.Util._
import services.{NotificationManager, LogActivity, CustomerWatcherNotFound, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.aliases._

object CustomerWatcherUpdater {

  def watch(admin: StoreAdmin, customerId: Int, requestedAssigneeIds: Seq[Int])
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[TheResponse[Root]] = (for {

    customer        ← * <~ Customers.mustFindById404(customerId)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    assignees       ← * <~ CustomerWatchers.watchersFor(customer).result.toXor
    newWatchers     = adminIds.diff(assignees.map(_.id))
      .map(adminId ⇒ CustomerWatcher(customerId = customer.id, watcherId = adminId))
    _               ← * <~ CustomerWatchers.createAll(newWatchers)
    newCustomer     ← * <~ Customers.refresh(customer).toXor
    response        ← * <~ CustomerResponse.fromCustomer(customer).toXor
    notFoundAdmins  = diffToFailures(requestedAssigneeIds, adminIds, StoreAdmin)
    assignedAdmins  = response.assignees.filter(a ⇒ newWatchers.map(_.watcherId).contains(a.assignee.id)).map(_.assignee)
    _               ← * <~ LogActivity.addedWatchersToCustomer(admin, customer, assignedAdmins)
    _               ← * <~ NotificationManager.subscribe(adminIds = assignedAdmins.map(_.id),
      dimension = Dimension.customer, reason = NotificationSubscription.Assigned, objectIds = Seq(customerId.toString))
  } yield TheResponse.build(response, errors = notFoundAdmins)).runTxn()

  def unwatch(admin: StoreAdmin, customerId: Int, assigneeId: Int)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[Root] = (for {

    customer ← * <~ Customers.mustFindById404(customerId)
    assignee ← * <~ StoreAdmins.mustFindById404(assigneeId)
    watcher  ← * <~ CustomerWatchers.byWatcher(assignee).one.mustFindOr(CustomerWatcherNotFound(customerId, assigneeId))
    _        ← * <~ CustomerWatchers.byWatcher(assignee).delete
    response ← * <~ CustomerResponse.fromCustomer(customer).toXor
    _        ← * <~ LogActivity.removedWatcherFromCustomer(admin, customer, assignee)
    _        ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assigneeId),
      dimension = Dimension.customer, reason = NotificationSubscription.Assigned, objectIds = Seq(customerId.toString))
  } yield response).runTxn()

  def watchBulk(admin: StoreAdmin, payload: CustomerBulkWatchersPayload)(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkCustomerUpdateResponse] = (for {

    // TODO: transfer sorting-paging metadata
    customers   ← * <~ Customers.filter(_.id.inSetBind(payload.customerIds)).result.toXor
    assignee    ← * <~ StoreAdmins.mustFindById400(payload.watcherId)
    newWatchers = for (c ← customers) yield CustomerWatcher(customerId = c.id, watcherId = assignee.id)
    _           ← * <~ CustomerWatchers.createAll(newWatchers)
    response    ← * <~ CustomerQueries.findAll
    success     = customers.filter(c ⇒ newWatchers.map(_.customerId).contains(c.id)).map(_.id)
    _           ← * <~ LogActivity.bulkAssignedToCustomers(admin, assignee, success)
    _           ← * <~ NotificationManager.subscribe(adminIds = Seq(assignee.id), dimension = Dimension.customer,
      reason = NotificationSubscription.Watching, objectIds = customers.map(_.id.toString)).value
    // Prepare batch response
    batchFailures  = diffToBatchErrors(payload.customerIds, customers.map(_.id), Customer)
    batchMetadata  = BatchMetadata(BatchMetadataSource(Customer, success.map(_.toString), batchFailures))
  } yield response.copy(errors = flattenErrors(batchFailures), batch = Some(batchMetadata))).runTxn()

  def unwatchBulk(admin: StoreAdmin, payload: CustomerBulkWatchersPayload)(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkCustomerUpdateResponse] = (for {

    // TODO: transfer sorting-paging metadata
    customers ← * <~ Customers.filter(_.id.inSetBind(payload.customerIds)).result
    assignee  ← * <~ StoreAdmins.mustFindById400(payload.watcherId)
    _         ← * <~ CustomerWatchers.filter(_.watcherId === payload.watcherId)
      .filter(_.customerId.inSetBind(customers.map(_.id))).delete
    response  ← * <~ CustomerQueries.findAll
    success   = customers.filter(c ⇒ payload.customerIds.contains(c.id)).map(_.id)
    _         ← * <~ LogActivity.bulkUnassignedFromCustomers(admin, assignee, success)
    _         ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assignee.id), dimension = Dimension.customer,
      reason = NotificationSubscription.Watching, objectIds = customers.map(_.id.toString)).value
    // Prepare batch response
    batchFailures  = diffToBatchErrors(payload.customerIds, customers.map(_.id), Customer)
    batchMetadata  = BatchMetadata(BatchMetadataSource(Customer, success.map(_.toString), batchFailures))
  } yield response.copy(errors = flattenErrors(batchFailures), batch = Some(batchMetadata))).runTxn()
}
