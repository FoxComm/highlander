package phoenix.models.customer

import java.time.Instant

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.Failures
import phoenix.failures.CustomerGroupFailures.CustomerGroupTypeIsWrong
import phoenix.models.customer.CustomerGroup._
import phoenix.payloads.CustomerGroupPayloads.CustomerGroupPayload
import phoenix.utils.ADT
import phoenix.utils.aliases._
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.lifted.Tag

case class CustomerGroup(id: Int = 0,
                         scope: LTree,
                         createdBy: Int,
                         name: String,
                         customersCount: Int = 0, // FIXME: is this denormalization needed at all? https://foxcommerce.slack.com/archives/C06696D1R/p1498564090580988 @michalrus
                         clientState: Json,
                         elasticRequest: Json,
                         groupType: GroupType,
                         updatedAt: Instant = Instant.now,
                         createdAt: Instant = Instant.now,
                         deletedAt: Option[Instant] = None)
    extends FoxModel[CustomerGroup] {

  def mustBeOfType(expected: GroupType): Either[Failures, CustomerGroup] =
    if (groupType == expected) Either.right(this)
    else Either.left(CustomerGroupTypeIsWrong(id, groupType, Set(expected)).single)

  def mustNotBeOfType(expectedNot: GroupType): Either[Failures, CustomerGroup] =
    if (groupType != expectedNot) Either.right(this)
    else
      Either.left(
        CustomerGroupTypeIsWrong(id, groupType, CustomerGroup.types.filterNot(_ == expectedNot).toSet).single)

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
    CustomerGroup(
      id = 0,
      scope = scope,
      createdBy = adminId,
      name = p.name,
      customersCount = p.customersCount,
      clientState = p.clientState,
      elasticRequest = p.elasticRequest,
      groupType = p.groupType
    )
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

  def fildAllByIds(ids: Set[Int]): QuerySeq =
    filter(_.id.inSet(ids))

  def fildAllByIdsAndType(ids: Set[Int], groupType: GroupType): QuerySeq =
    findAllByIds(ids).filter(_.groupType === groupType)
}
