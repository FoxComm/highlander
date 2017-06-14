package phoenix.services.assignments

import core.db._
import phoenix.models.account._
import phoenix.models.activity.Dimension
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.users.UserResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object CustomerAssignmentsManager extends AssignmentsManager[Int, User] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Customer
  val notifyDimension = Dimension.customer
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: User): UserResponse = UserResponse.build(model)

  def fetchEntity(accountId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[User] =
    Users.mustFindByAccountId(accountId)

  def fetchSequence(accountIds: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[User]] =
    Users.filter(_.accountId.inSetBind(accountIds)).result.dbresult
}
