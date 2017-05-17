package services.assignments

import models.{Assignment, NotificationSubscription}
import models.account._
import responses.UserResponse.{Root, build}
import slick.jdbc.PostgresProfile.api._
import utils.db._
import utils.aliases._

object CustomerWatchersManager extends AssignmentsManager[Int, User] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Customer
  val notifyDimension = models.activity.Dimension.customer
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: User): Root = build(model)

  def fetchEntity(accountId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[User] =
    Users.mustFindByAccountId(accountId)

  def fetchSequence(accountIds: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[User]] =
    Users.filter(_.accountId.inSetBind(accountIds)).result.dbresult
}
