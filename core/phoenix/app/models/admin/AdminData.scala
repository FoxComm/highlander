package models.admin

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import com.pellucid.sealerate
import failures.Failure
import failures.UserFailures._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Passwords.hashPassword
import utils.aliases._
import utils.db._
import utils.{ADT, FSM, Validation}

import models.admin.AdminData._

case class AdminData(id: Int = 0,
                     userId: Int,
                     accountId: Int,
                     state: State = Inactive,
                     createdAt: Instant = Instant.now,
                     udpatedAt: Instant = Instant.now,
                     deletedAt: Option[Instant] = None)
    extends FoxModel[AdminData]
    with Validation[AdminData]
    with FSM[AdminData.State, AdminData] {

  import Validation._

  def stateLens = lens[AdminData].state

  val fsm: Map[State, Set[State]] = Map(
      Invited  → Set(Active, Inactive, Archived),
      Active   → Set(Inactive, Archived),
      Inactive → Set(Active, Archived)
  )

  private val canLoginWithStates = Set(Active, Invited)

  def canLogin: Boolean = canLoginWithStates.toSeq.contains(state)
}

object AdminData {

  sealed trait State

  case object Invited  extends State
  case object Active   extends State
  case object Inactive extends State
  case object Archived extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
}

class AdminsData(tag: Tag) extends FoxTable[AdminData](tag, "admin_data") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId    = column[Int]("user_id")
  def accountId = column[Int]("account_id")
  def state     = column[AdminData.State]("state")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, userId, accountId, state, createdAt, updatedAt, deletedAt) <> ((AdminData.apply _).tupled, AdminData.unapply)
}

object AdminsData
    extends FoxTableQuery[AdminData, AdminsData](new AdminsData(_))
    with ReturningId[AdminData, AdminsData] {

  val returningLens: Lens[AdminData, Int] = lens[AdminData].id

  def findOneByAccountId(accountId: Int): DBIO[Option[AdminData]] =
    filter(_.accountId === accountId).result.headOption

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def mustFindByAccountId(accountId: Int)(implicit ec: EC): DbResultT[AdminData] =
    filter(_.accountId === accountId).mustFindOneOr(UserWithAccountNotFound(accountId))
}
