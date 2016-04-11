package services.assignments

import models.{Assignment, NotificationSubscription}
import models.payment.giftcard._
import responses.GiftCardResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object GiftCardAssignmentsManager extends AssignmentsManager[String, GiftCard] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.GiftCard
  val notifyDimension = models.activity.Dimension.giftCard
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: GiftCard): Root = build(model)

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResult[GiftCard] =
    GiftCards.mustFindByCode(code)

  def fetchSequence(codes: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[GiftCard]] =
    GiftCards.filter(_.code.inSetBind(codes)).result.toXor
}
