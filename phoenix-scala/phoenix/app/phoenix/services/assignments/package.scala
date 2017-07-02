package phoenix.services

import cats.implicits._
import core.db._
import phoenix.models.account.User
import phoenix.responses.TheResponse
import phoenix.utils.aliases._

package object assignments {
  // Notification (un)subscription helpers
  def subscribe[K, M <: FoxModel[M]](
      manager: AssignmentsManager[K, M],
      adminIds: Seq[Int],
      objectIds: Seq[String])(implicit ec: EC): DbResultT[TheResponse[Option[Int]]] =
    if (objectIds.nonEmpty)
      NotificationManager.subscribe(adminIds = adminIds,
                                    dimension = manager.notifyDimension,
                                    reason = manager.notifyReason,
                                    objectIds = objectIds)
    else
      TheResponse(none[Int]).pure[DbResultT]

  def unsubscribe[K, M <: FoxModel[M]](manager: AssignmentsManager[K, M],
                                       adminIds: Seq[Int],
                                       objectIds: Seq[String])(implicit ec: EC): DbResultT[Unit] =
    when(
      objectIds.nonEmpty,
      NotificationManager.unsubscribe(adminIds = adminIds,
                                      dimension = manager.notifyDimension,
                                      reason = manager.notifyReason,
                                      objectIds = objectIds)
    )

  // Activity logger helpers
  def logBulkAssign[K, M <: FoxModel[M]](manager: AssignmentsManager[K, M],
                                         originator: User,
                                         admin: User,
                                         keys: Seq[String])(implicit ec: EC, ac: AC) =
    when(keys.nonEmpty,
         LogActivity()
           .bulkAssigned(originator, admin, keys, manager.assignmentType, manager.referenceType)
           .void)

  def logBulkUnassign[K, M <: FoxModel[M]](manager: AssignmentsManager[K, M],
                                           originator: User,
                                           admin: User,
                                           keys: Seq[String])(implicit ec: EC, ac: AC) =
    when(keys.nonEmpty,
         LogActivity()
           .bulkUnassigned(originator, admin, keys, manager.assignmentType, manager.referenceType)
           .void)
}
