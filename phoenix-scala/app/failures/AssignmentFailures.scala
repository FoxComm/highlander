package failures

import failures.Util._
import utils.friendlyClassName

object AssignmentFailures {

  case class AlreadyAssignedFailure(message: String) extends Failure {
    override def description = message
  }

  object AlreadyAssignedFailure {
    def apply[A](a: A, searchKey: Any, storeAdminId: Int): AlreadyAssignedFailure = {
      val model = friendlyClassName(a)
      val term  = searchTerm(a)
      AlreadyAssignedFailure(
        s"storeAdmin with id=$storeAdminId is already assigned to $model with $term=$searchKey")
    }
  }

  case class NotAssignedFailure(message: String) extends Failure {
    override def description = message
  }

  object NotAssignedFailure {
    def apply[A](a: A, searchKey: Any, storeAdminId: Int): NotAssignedFailure = {
      val model = friendlyClassName(a)
      val term  = searchTerm(a)
      NotAssignedFailure(
        s"storeAdmin with id=$storeAdminId is not assigned to $model with $term=$searchKey")
    }
  }

  case class AssigneeNotFoundFailure(message: String) extends Failure {
    override def description = message
  }

  object AssigneeNotFoundFailure {
    def apply[A](a: A, searchKey: Any, assigneeId: Int): AssigneeNotFoundFailure = {
      val model = friendlyClassName(a)
      val term  = searchTerm(a)
      AssigneeNotFoundFailure(
        s"storeAdmin with id=$assigneeId is not assigned to $model with $term=$searchKey")
    }
  }
}
