package responses

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.{StoreAdmin, StoreAdmins}
import models.payment.giftcard.{GiftCard, GiftCardAssignments, GiftCardAssignment, GiftCardWatchers, GiftCardWatcher}
import slick.driver.PostgresDriver.api._
import utils.Money._

object GiftCardResponse {
  final val mockMessage = "Not implemented yet"

  final case class BulkCreateResponse(responses: Seq[Root]) extends ResponseItem

  final case class Root(
    id: Int,
    createdAt: Instant,
    code: String,
    originId: Int,
    originType: GiftCard.OriginType,
    subTypeId: Option[Int],
    state: GiftCard.State,
    currency: Currency,
    originalBalance: Int,
    availableBalance: Int,
    currentBalance: Int,
    canceledAmount: Option[Int],
    canceledReason: Option[Int],
    customer: Option[CustomerResponse.Root],
    storeAdmin: Option[StoreAdminResponse.Root],
    message: String,
    assignees: Seq[AssignmentResponse.Root] = Seq.empty,
    watchers: Seq[WatcherResponse.Root] = Seq.empty) extends ResponseItem

  final case class RootSimple(
    id: Int,
    createdAt: Instant,
    code: String,
    originId: Int,
    originType: GiftCard.OriginType,
    subTypeId: Option[Int],
    state: GiftCard.State,
    currency: Currency,
    originalBalance: Int,
    availableBalance: Int,
    currentBalance: Int,
    canceledAmount: Option[Int],
    canceledReason: Option[Int],
    message: String) extends ResponseItem

  def build(gc: GiftCard, customer: Option[CustomerResponse.Root] = None,
    admin: Option[StoreAdminResponse.Root] = None, assignments: Seq[(GiftCardAssignment, StoreAdmin)] = Seq.empty,
    watchers: Seq[(GiftCardWatcher, StoreAdmin)] = Seq.empty): Root =
    Root(
      id = gc.id,
      createdAt = gc.createdAt,
      code = gc.code,
      originId = gc.originId,
      originType = gc.originType,
      subTypeId = gc.subTypeId,
      state = gc.state,
      currency = gc.currency,
      originalBalance = gc.originalBalance,
      availableBalance = gc.availableBalance,
      currentBalance = gc.currentBalance,
      canceledAmount = gc.canceledAmount,
      canceledReason = gc.canceledReason,
      customer = customer,
      storeAdmin = admin,
      message = mockMessage,
      assignees = assignments.map((AssignmentResponse.buildForGiftCard _).tupled),
      watchers = watchers.map((WatcherResponse.buildForGiftCard _).tupled))

  def buildForList(gc: GiftCard): RootSimple =
    RootSimple(
      id = gc.id,
      createdAt = gc.createdAt,
      code = gc.code,
      originId = gc.originId,
      originType = gc.originType,
      subTypeId = gc.subTypeId,
      state = gc.state,
      currency = gc.currency,
      originalBalance = gc.originalBalance,
      availableBalance = gc.availableBalance,
      currentBalance = gc.currentBalance,
      canceledAmount = gc.canceledAmount,
      canceledReason = gc.canceledReason,
      message = mockMessage)

  def fromGiftCard(gc: GiftCard)(implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchDetails(gc).map {
      case (assignees, watchers) ⇒
        build(
          gc = gc,
          customer = None, // FIXME
          admin = None,    // FIXME
          assignments = assignees,
          watchers = watchers
        )
    }
  }

  private def fetchDetails(gc: GiftCard)(implicit ec: ExecutionContext, db: Database) = {
    for {
      assignments ← GiftCardAssignments.filter(_.giftCardId === gc.id).result
      admins      ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
      watchlist   ← GiftCardWatchers.filter(_.giftCardId === gc.id).result
      watchers    ← StoreAdmins.filter(_.id.inSetBind(watchlist.map(_.watcherId))).result
    } yield (assignments.zip(admins), watchlist.zip(watchers))
  }
}
