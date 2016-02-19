package models.payment.giftcard

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.{StoreAdmin, StoreAdmins, javaTimeSlickMapper}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class GiftCardAssignment(id: Int = 0, giftCardId: Int, assigneeId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[GiftCardAssignment]

object GiftCardAssignment

class GiftCardAssignments(tag: Tag) extends GenericTable.TableWithId[GiftCardAssignment](tag, "gift_card_assignments") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")
  def assigneeId = column[Int]("assignee_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, giftCardId, assigneeId, createdAt) <> ((GiftCardAssignment.apply _).tupled, GiftCardAssignment.unapply)
  def giftCard = foreignKey(GiftCards.tableName, giftCardId, GiftCards)(_.id)
  def assignee = foreignKey(StoreAdmins.tableName, assigneeId, StoreAdmins)(_.id)
}

object GiftCardAssignments extends TableQueryWithId[GiftCardAssignment, GiftCardAssignments](
  idLens = GenLens[GiftCardAssignment](_.id)
)(new GiftCardAssignments(_)) {

  def byAssignee(admin: StoreAdmin): QuerySeq = filter(_.assigneeId === admin.id)

  def assignedTo(admin: StoreAdmin)(implicit ec: ExecutionContext): GiftCards.QuerySeq = {
    for {
      assignees ← byAssignee(admin).map(_.giftCardId)
      giftCards ← GiftCards.filter(_.id === assignees)
    } yield giftCards
  }

  def byGiftCard(customer: GiftCard): QuerySeq = filter(_.giftCardId === customer.id)

  def assigneesFor(customer: GiftCard)(implicit ec: ExecutionContext): StoreAdmins.QuerySeq = {
    for {
      assignees ← byGiftCard(customer).map(_.assigneeId)
      admins    ← StoreAdmins.filter(_.id === assignees)
    } yield admins
  }
}
