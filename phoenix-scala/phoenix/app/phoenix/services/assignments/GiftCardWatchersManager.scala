package phoenix.services.assignments

import core.db._
import phoenix.models.activity.Dimension
import phoenix.models.payment.giftcard._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.giftcards.GiftCardResponse
import phoenix.responses.giftcards.GiftCardResponse._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object GiftCardWatchersManager extends AssignmentsManager[String, GiftCard] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.GiftCard
  val notifyDimension = Dimension.giftCard
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: GiftCard): GiftCardResponse = build(model)

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[GiftCard] =
    GiftCards.mustFindByCode(code)

  def fetchSequence(codes: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[GiftCard]] =
    GiftCards.filter(_.code.inSetBind(codes)).result.dbresult
}
