package failures

import utils.friendlyClassName
import Util._

final case class AlreadyAssignedFailure(message: String) extends Failure {
  override def description = message
}

object AlreadyAssignedFailure {
  def apply[A](a: A, searchKey: Any, storeAdminId: Int): AlreadyAssignedFailure = {
    val msg = s"storeAdmin with id=$storeAdminId is already assigned to ${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey"
    AlreadyAssignedFailure(msg)
  }
}

final case class NotAssignedFailure(message: String) extends Failure {
  override def description = message
}

object NotAssignedFailure {
  def apply[A](a: A, searchKey: Any, storeAdminId: Int): NotAssignedFailure = {
    val msg = s"storeAdmin with id=$storeAdminId is not assigned to ${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey"
    NotAssignedFailure(msg)
  }
}
