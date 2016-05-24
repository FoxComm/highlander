package services

import models.StoreAdmin
import responses.TheResponse
import utils.aliases._
import utils.db._

package object assignments {
  // Notification (un)subscription helpers
  def subscribe[K, M <: FoxModel[M]](
      manager: AssignmentsManager[K, M], adminIds: Seq[Int], objectIds: Seq[String])(
      implicit ec: EC): DbResult[TheResponse[Option[Int]]] = {

    if (objectIds.nonEmpty)
      NotificationManager
        .subscribe(adminIds = adminIds,
                   dimension = manager.notifyDimension,
                   reason = manager.notifyReason,
                   objectIds = objectIds)
        .value
    else
      DbResult.good(TheResponse(None))
  }

  def unsubscribe[K, M <: FoxModel[M]](
      manager: AssignmentsManager[K, M], adminIds: Seq[Int], objectIds: Seq[String])(
      implicit ec: EC): DbResult[Unit] = {

    if (objectIds.nonEmpty)
      NotificationManager
        .unsubscribe(adminIds = adminIds,
                     dimension = manager.notifyDimension,
                     reason = manager.notifyReason,
                     objectIds = objectIds)
        .value
    else
      DbResult.unit
  }

  // Activity logger helpers
  def logBulkAssign[K, M <: FoxModel[M]](manager: AssignmentsManager[K, M],
                                         originator: StoreAdmin,
                                         admin: StoreAdmin,
                                         keys: Seq[String])(implicit ec: EC, ac: AC) = {

    if (keys.nonEmpty)
      LogActivity.bulkAssigned(
          originator, admin, keys, manager.assignmentType, manager.referenceType)
    else
      DbResult.unit
  }

  def logBulkUnassign[K, M <: FoxModel[M]](manager: AssignmentsManager[K, M],
                                           originator: StoreAdmin,
                                           admin: StoreAdmin,
                                           keys: Seq[String])(implicit ec: EC, ac: AC) = {

    if (keys.nonEmpty)
      LogActivity.bulkUnassigned(
          originator, admin, keys, manager.assignmentType, manager.referenceType)
    else
      DbResult.unit
  }
}
