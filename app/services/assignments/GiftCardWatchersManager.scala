package services.assignments

import models.Assignment
import models.payment.giftcard._
import responses.GiftCardResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object GiftCardWatchersManager extends AssignmentsManager[String, GiftCard] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Watcher
  def referenceType(): Assignment.ReferenceType = Assignment.GiftCard
  def notifyDimension(): String = models.activity.Dimension.giftCard

  def buildResponse(model: GiftCard): Root = build(model)

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResult[GiftCard] =
    GiftCards.mustFindByCode(code)

  def fetchSequence(codes: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[GiftCard]] =
    GiftCards.filter(_.code.inSetBind(codes)).result.toXor
}
