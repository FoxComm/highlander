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

import models.admin.StoreAdminUser._

case class StoreAdminUser(id: Int = 0,
                          userId: Int,
                          accountId: Int,
                          state: State = Inactive,
                          createdAt: Instant = Instant.now,
                          udpatedAt: Instant = Instant.now,
                          deletedAt: Option[Instant] = None)
    extends FoxModel[StoreAdminUser]
    with Validation[StoreAdminUser]
    with FSM[StoreAdminUser.State, StoreAdminUser] {

  import Validation._

  def stateLens = lens[StoreAdminUser].state

  val fsm: Map[State, Set[State]] = Map(
      Invited  → Set(Active, Inactive, Archived),
      Active   → Set(Inactive, Archived),
      Inactive → Set(Active, Archived)
  )

  private val canLoginWithStates = Set(Active, Invited)

  def canLogin: Boolean = canLoginWithStates.toSeq.contains(state)
}

object StoreAdminUser {

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

class StoreAdminUsers(tag: Tag) extends FoxTable[StoreAdminUser](tag, "store_admin_users") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId    = column[Int]("user_id")
  def accountId = column[Int]("account_id")
  def state     = column[StoreAdminUser.State]("state")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, userId, accountId, state, createdAt, updatedAt, deletedAt) <> ((StoreAdminUser.apply _).tupled, StoreAdminUser.unapply)
}

object StoreAdminUsers
    extends FoxTableQuery[StoreAdminUser, StoreAdminUsers](new StoreAdminUsers(_))
    with ReturningId[StoreAdminUser, StoreAdminUsers] {

  val returningLens: Lens[StoreAdminUser, Int] = lens[StoreAdminUser].id

  def findOneByAccountId(accountId: Int): DBIO[Option[StoreAdminUser]] =
    filter(_.accountId === accountId).result.headOption

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def mustFindByAccountId(accountId: Int)(implicit ec: EC): DbResultT[StoreAdminUser] =
    filter(_.accountId === accountId).mustFindOneOr(UserWithAccountNotFound(accountId))
}
