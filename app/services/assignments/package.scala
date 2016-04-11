package services

import models.StoreAdmin
import responses.TheResponse
import utils.ModelWithIdParameter
import utils.Slick.{DbResult, _}
import utils.aliases._

package object assignments {
  // Notification (un)subscription helpers
  def subscribe[K, M <: ModelWithIdParameter[M]](manager: AssignmentsManager[K, M], adminIds: Seq[Int],
    objectIds: Seq[String])(implicit ec: EC, db: DB): DbResult[TheResponse[Option[Int]]] = {

    if (objectIds.nonEmpty)
      NotificationManager.subscribe(adminIds = adminIds, dimension = manager.notifyDimension,
        reason = manager.notifyReason, objectIds = objectIds).value
    else
      DbResult.good(TheResponse(None))
  }


  def unsubscribe[K, M <: ModelWithIdParameter[M]](manager: AssignmentsManager[K, M], adminIds: Seq[Int],
    objectIds: Seq[String])(implicit ec: EC, db: DB): DbResult[Unit] = {

    if (objectIds.nonEmpty)
      NotificationManager.unsubscribe(adminIds = adminIds, dimension = manager.notifyDimension,
        reason = manager.notifyReason, objectIds = objectIds).value
    else
      DbResult.unit
  }

  // Activity logger helpers
  def logBulkAssign[K, M <: ModelWithIdParameter[M]](manager: AssignmentsManager[K, M],
    originator: StoreAdmin, admin: StoreAdmin, keys: Seq[String])(implicit ec: EC, db: DB, ac: AC) = {

    if (keys.nonEmpty)
      LogActivity.bulkAssigned(originator, admin, keys, manager.assignmentType, manager.referenceType)
    else
      DbResult.unit
  }

  def logBulkUnassign[K, M <: ModelWithIdParameter[M]](manager: AssignmentsManager[K, M],
    originator: StoreAdmin, admin: StoreAdmin, keys: Seq[String])(implicit ec: EC, db: DB, ac: AC) = {

    if (keys.nonEmpty)
      LogActivity.bulkUnassigned(originator, admin, keys, manager.assignmentType, manager.referenceType)
    else
      DbResult.unit
  }
}
