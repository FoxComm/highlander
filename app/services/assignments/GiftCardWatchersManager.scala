package services.assignments

import models.Assignment
import models.payment.giftcard._
import utils.Slick._
import utils.aliases._

object GiftCardWatchersManager extends AssignmentsManager[String, GiftCard] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Watcher
  def referenceType(): Assignment.ReferenceType = Assignment.GiftCard
  def notifyDimension(): String = models.activity.Dimension.giftCard

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResult[GiftCard] =
    GiftCards.mustFindByCode(code)
}
