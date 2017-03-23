package models.customer

import java.time.Instant

import shapeless._
import slick.lifted.Tag
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class CustomerGroupMember(id: Int = 0,
                               customerDataId: Int,
                               groupId: Int,
                               createdAt: Instant = Instant.now)
    extends FoxModel[CustomerGroupMember]

class CustomerGroupMembers(tag: Tag)
    extends FoxTable[CustomerGroupMember](tag, "customer_group_members") {

  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def groupId        = column[Int]("group_id")
  def customerDataId = column[Int]("customer_data_id")
  def createdAt      = column[Instant]("created_at")

  def * =
    (id, customerDataId, groupId, createdAt) <>
      ((CustomerGroupMember.apply _).tupled, CustomerGroupMember.unapply)
}

object CustomerGroupMembers
    extends FoxTableQuery[CustomerGroupMember, CustomerGroupMembers](new CustomerGroupMembers(_))
    with ReturningId[CustomerGroupMember, CustomerGroupMembers] {

  def findByGroupId(groupId: Int): QuerySeq =
    filter(_.groupId === groupId)

  def findByCustomerDataId(customerDataId: Int): QuerySeq =
    filter(_.customerDataId === customerDataId)

  def findByGroupIdAndCustomerDataId(customerDataId: Int, groupId: Int) =
    filter(_.groupId === groupId).filter(_.customerDataId === customerDataId)

  val returningLens: Lens[CustomerGroupMember, Int] = lens[CustomerGroupMember].id

}
