package failures

import utils.friendlyClassName
import Util.searchTerm

object LockFailures {

  final case class LockedFailure(message: String) extends Failure {
    override def description = message
  }

  object LockedFailure {
    def apply[A](a: A, searchKey: Any): LockedFailure = {
      LockedFailure(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey is locked")
    }
  }

  final case class NotLockedFailure(message: String) extends Failure {
    override def description = message
  }

  object NotLockedFailure {
    def apply[A](a: A, searchKey: Any): NotLockedFailure = {
      NotLockedFailure(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey is not locked")
    }
  }

}
