package models.customer

import java.time.Instant

import com.pellucid.sealerate
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.ADT
import utils.db._
import CustomerPasswordReset.{Initial, State}
import failures.CustomerFailures.PasswordResetAlreadyInitiated
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.aliases.EC
import utils.generateUuid

case class CustomerPasswordReset(id: Int = 0,
                                 customerId: Int,
                                 email: String,
                                 state: State = Initial,
                                 code: String,
                                 createdAt: Instant = Instant.now)
    extends FoxModel[CustomerPasswordReset] {}

object CustomerPasswordReset {

  sealed trait State

  case object Initial          extends State
  case object EmailSended      extends State
  case object PasswordRestored extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

  def optionFromCustomer(customer: Customer): Option[CustomerPasswordReset] = {
    customer.email.map { email ⇒
      CustomerPasswordReset(customerId = customer.id, code = generateUuid, email = email)
    }
  }
}

class CustomerPasswordResets(tag: Tag)
    extends FoxTable[CustomerPasswordReset](tag, "customer_password_resets") {

  import CustomerPasswordReset._

  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def email      = column[String]("email")
  def state      = column[CustomerPasswordReset.State]("state")
  def code       = column[String]("code")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, customerId, email, state, code, createdAt) <> ((CustomerPasswordReset.apply _).tupled, CustomerPasswordReset.unapply)
}

object CustomerPasswordResets
    extends FoxTableQuery[CustomerPasswordReset, CustomerPasswordResets](
        new CustomerPasswordResets(_))
    with ReturningId[CustomerPasswordReset, CustomerPasswordResets] {

  val returningLens: Lens[CustomerPasswordReset, Int] = lens[CustomerPasswordReset].id

  object scope {
    import CustomerPasswordReset._

    implicit class QuerySeqConversions(query: QuerySeq) {

      def findActiveByEmail(email: String): QuerySeq = {
        filter(c ⇒ c.email === email && c.state =!= (PasswordRestored: State))
      }

      def mustBeNotActivated(email: String)(implicit ec: EC): DbResultT[Unit] = {
        findActiveByEmail(email).one.mustNotFindOr(PasswordResetAlreadyInitiated(email))
      }
    }
  }
}
