package models.customer

import java.time.Instant

import cats.data.Xor
import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import failures.CustomerGroupFailures.CustomerGroupTypeIsWrong
import failures.Failures
import models.account.Scope
import models.customer.CustomerGroup._
import payloads.CustomerGroupPayloads.CustomerGroupPayload
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.lifted.Tag
import utils.ADT
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class CustomerGroup(id: Int = 0,
                         scope: LTree,
                         createdBy: Int,
                         name: String,
                         customersCount: Int = 0,
                         clientState: Json,
                         elasticRequest: Json,
                         groupType: GroupType,
                         updatedAt: Instant = Instant.now,
                         createdAt: Instant = Instant.now,
                         deletedAt: Option[Instant] = None)
    extends FoxModel[CustomerGroup] {

  def mustBeOfType(expected: GroupType): Failures Xor CustomerGroup = {
    if (groupType == expected) Xor.Right(this)
    else Xor.Left(CustomerGroupTypeIsWrong(id, groupType, Set(expected)).single)
  }

  def mustNotBeOfType(expectedNot: GroupType): Failures Xor CustomerGroup = {
    if (groupType != expectedNot) Xor.Right(this)
    else
      Xor.Left(
          CustomerGroupTypeIsWrong(id,
                                   groupType,
                                   CustomerGroup.types.filterNot(_ == expectedNot).toSet).single)
  }

}

object CustomerGroup {
  sealed trait GroupType

  case object Manual   extends GroupType
  case object Dynamic  extends GroupType
  case object Template extends GroupType

  object GroupType extends ADT[GroupType] {
    def types = sealerate.values[GroupType]
  }

  val types = Seq(Manual, Dynamic, Template)

  implicit val stateColumnType: JdbcType[GroupType] with BaseTypedType[GroupType] =
    GroupType.slickColumn

  def fromPayloadAndAdmin(p: CustomerGroupPayload, adminId: Int, scope: LTree): CustomerGroup =
    CustomerGroup(id = 0,
                  scope = scope,
                  createdBy = adminId,
                  name = p.name,
                  customersCount = p.customersCount,
                  clientState = p.clientState,
                  elasticRequest = p.elasticRequest,
                  groupType = p.`type`)
}

class CustomerGroups(tag: Tag) extends FoxTable[CustomerGroup](tag, "customer_groups") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope          = column[LTree]("scope")
  def createdBy      = column[Int]("created_by")
  def name           = column[String]("name")
  def customersCount = column[Int]("customers_count")
  def clientState    = column[Json]("client_state")
  def elasticRequest = column[Json]("elastic_request")
  def groupType      = column[GroupType]("group_type")
  def updatedAt      = column[Instant]("updated_at")
  def createdAt      = column[Instant]("created_at")
  def deletedAt      = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     scope,
     createdBy,
     name,
     customersCount,
     clientState,
     elasticRequest,
     groupType,
     updatedAt,
     createdAt,
     deletedAt) <>
      ((CustomerGroup.apply _).tupled, CustomerGroup.unapply)
}

object CustomerGroups
    extends FoxTableQuery[CustomerGroup, CustomerGroups](new CustomerGroups(_))
    with ReturningId[CustomerGroup, CustomerGroups] {

  val returningLens: Lens[CustomerGroup, Int] = lens[CustomerGroup].id

  def filterActive(): QuerySeq = filter(_.deletedAt.isEmpty)

  def fildAllByIdsAndType(ids: Set[Int], groupType: GroupType) =
    filter(_.id.inSet(ids)).filter(_.groupType === groupType)
}
