package models.account

import java.time.Instant

import models.account._

import com.pellucid.sealerate
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.ADT
import utils.db._
import UserPasswordReset.{Initial, State}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.generateUuid

case class UserPasswordReset(id: Int = 0,
                             accountId: Int,
                             email: String,
                             state: State = Initial,
                             code: String,
                             activatedAt: Option[Instant] = None,
                             createdAt: Instant = Instant.now)
    extends FoxModel[UserPasswordReset] {

  def updateCode(): UserPasswordReset = this.copy(code = generateUuid)

}

object UserPasswordReset {

  sealed trait State

  case object Initial          extends State
  case object EmailSend        extends State
  case object Disabled         extends State
  case object PasswordRestored extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

  def optionFromUser(user: User): Option[UserPasswordReset] = {
    user.email.map { email ⇒
      UserPasswordReset(accountId = user.accountId, code = generateUuid, email = email)
    }
  }
}

class UserPasswordResets(tag: Tag)
    extends FoxTable[UserPasswordReset](tag, "user_password_resets") {

  import UserPasswordReset._

  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId   = column[Int]("account_id")
  def email       = column[String]("email")
  def state       = column[UserPasswordReset.State]("state")
  def code        = column[String]("code")
  def activatedAt = column[Option[Instant]]("activated_at")
  def createdAt   = column[Instant]("created_at")

  def * =
    (id, accountId, email, state, code, activatedAt, createdAt) <> ((UserPasswordReset.apply _).tupled,
    UserPasswordReset.unapply)
}

object UserPasswordResets
    extends FoxTableQuery[UserPasswordReset, UserPasswordResets](new UserPasswordResets(_))
    with ReturningId[UserPasswordReset, UserPasswordResets] {

  val returningLens: Lens[UserPasswordReset, Int] = lens[UserPasswordReset].id

  def findActiveByCode(code: String): DBIO[Option[UserPasswordReset]] =
    filter(c ⇒ c.code === code && c.state === (Initial: State)).one

  def findActiveByEmail(email: String): QuerySeq =
    filter(c ⇒ c.email === email && c.state === (Initial: State))

}
