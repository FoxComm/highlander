package models

import cats.data.ValidatedNel
import cats.implicits._
import com.pellucid.sealerate
import failures.Failure
import utils.Passwords.hashPassword
import utils.{ADT, FSM, Validation}
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.aliases._
import utils.db._
import models.StoreAdmin._

case class StoreAdmin(id: Int = 0,
                      name: String,
                      email: String,
                      phoneNumber: Option[String] = None,
                      hashedPassword: Option[String] = None,
                      department: Option[String] = None,
                      state: State = Invited)
    extends FoxModel[StoreAdmin]
    with Validation[StoreAdmin]
    with FSM[StoreAdmin.State, StoreAdmin] {

  import Validation._

  override def validate: ValidatedNel[Failure, StoreAdmin] = {
    (notEmpty(name, "name") |@| notEmpty(email, "email")).map { case _ ⇒ this }
  }

  def stateLens = lens[StoreAdmin].state

  val fsm: Map[State, Set[State]] = Map(
      Invited  → Set(Active, Inactive, Archived),
      Active   → Set(Inactive, Archived),
      Inactive → Set(Active, Archived)
  )

  private val canLoginWithStates = Set(Active, Invited)

  def canLogin: Boolean = canLoginWithStates.toSeq.contains(state)
}

object StoreAdmin {

  sealed trait State

  case object Invited  extends State
  case object Active   extends State
  case object Inactive extends State
  case object Archived extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
  def build(id: Int = 0,
            name: String,
            email: String,
            state: State,
            phoneNumber: Option[String] = None,
            password: Option[String] = None,
            department: Option[String] = None): StoreAdmin = {
    val passwordHash = password.map(hashPassword)
    StoreAdmin(id = id,
               email = email,
               name = name,
               phoneNumber = phoneNumber,
               hashedPassword = passwordHash,
               department = department,
               state = state)
  }
}

class StoreAdmins(tag: Tag) extends FoxTable[StoreAdmin](tag, "store_admins") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name           = column[String]("name")
  def email          = column[String]("email")
  def phoneNumber    = column[Option[String]]("phone_number")
  def hashedPassword = column[Option[String]]("hashed_password")
  def department     = column[Option[String]]("department")
  def state          = column[StoreAdmin.State]("state")

  def * =
    (id, name, email, phoneNumber, hashedPassword, department, state) <> ((StoreAdmin.apply _).tupled, StoreAdmin.unapply)
}

object StoreAdmins
    extends FoxTableQuery[StoreAdmin, StoreAdmins](new StoreAdmins(_))
    with ReturningId[StoreAdmin, StoreAdmins] {

  val returningLens: Lens[StoreAdmin, Int] = lens[StoreAdmin].id

  def findByEmail(email: String): DBIO[Option[StoreAdmin]] = {
    filter(_.email === email).one
  }
}
