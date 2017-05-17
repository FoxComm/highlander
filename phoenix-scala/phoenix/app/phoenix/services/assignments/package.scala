package services

import models.account.User
import responses.TheResponse
import utils.aliases._
import utils.db._

package object assignments {
  // Notification (un)subscription helpers
  def subscribe[K, M <: FoxModel[M]](
      manager: AssignmentsManager[K, M],
      adminIds: Seq[Int],
      objectIds: Seq[String])(implicit ec: EC): DbResultT[TheResponse[Option[Int]]] = {

    if (objectIds.nonEmpty)
      NotificationManager.subscribe(adminIds = adminIds,
                                    dimension = manager.notifyDimension,
                                    reason = manager.notifyReason,
                                    objectIds = objectIds)
    else
      DbResultT.good(TheResponse(None))
  }

  def unsubscribe[K, M <: FoxModel[M]](
      manager: AssignmentsManager[K, M],
      adminIds: Seq[Int],
      objectIds: Seq[String])(implicit ec: EC): DbResultT[Unit] = {

    if (objectIds.nonEmpty)
      NotificationManager.unsubscribe(adminIds = adminIds,
                                      dimension = manager.notifyDimension,
                                      reason = manager.notifyReason,
                                      objectIds = objectIds)
    else
      DbResultT.unit
  }

  // Activity logger helpers
  def logBulkAssign[K, M <: FoxModel[M]](manager: AssignmentsManager[K, M],
                                         originator: User,
                                         admin: User,
                                         keys: Seq[String])(implicit ec: EC, ac: AC) = {

    if (keys.nonEmpty)
      LogActivity()
        .bulkAssigned(originator, admin, keys, manager.assignmentType, manager.referenceType)
    else
      DbResultT.unit
  }

  def logBulkUnassign[K, M <: FoxModel[M]](manager: AssignmentsManager[K, M],
                                           originator: User,
                                           admin: User,
                                           keys: Seq[String])(implicit ec: EC, ac: AC) = {

    if (keys.nonEmpty)
      LogActivity()
        .bulkUnassigned(originator, admin, keys, manager.assignmentType, manager.referenceType)
    else
      DbResultT.unit
  }
}
