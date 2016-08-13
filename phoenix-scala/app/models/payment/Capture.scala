package models.payment

import java.time.Instant

import cats.data.Validated._
import cats.data.{ValidatedNel, Xor}
import cats.implicits._
import com.pellucid.sealerate
import failures.{Failure, Failures, GeneralFailure}
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Money._
import utils.Validation._
import utils._
import utils.aliases._
import utils.db._

case class Capture(id: Int = 0,
                   reference: String = "",
                   orderRef: String,
                   customerId: Int,
                   event: Capture.Event,
                   amount: Int,
                   currency: Currency = Currency.USD,
                   error: Option[String],
                   createdAt: Instant = Instant.now())
    extends FoxModel[Capture]

object Capture {
  sealed trait Event
  case object Captured extends Event
  case object Error    extends Event

  object Event extends ADT[Event] {
    def types = sealerate.values[Event]
  }

  implicit val stateColumnType: JdbcType[Event] with BaseTypedType[Event] = Event.slickColumn
}

class Captures(tag: Tag) extends FoxTable[Capture](tag, "captures") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def ref        = column[String]("ref")
  def orderRef   = column[String]("order_ref")
  def customerId = column[Int]("customer_id")
  def event      = column[Capture.Event]("event")
  def amount     = column[Int]("amount")
  def currency   = column[Currency]("currency")
  def error      = column[Option[String]]("error")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, ref, orderRef, customerId, event, amount, currency, error, createdAt) <> ((Capture.apply _).tupled, Capture.unapply)
}

object Captures
    extends FoxTableQuery[Capture, Captures](new Captures(_))
    with ReturningId[Capture, Captures] {

  def findActiveById(id: Int): QuerySeq      = filter(_.id === id)
  def findActiveByRef(ref: String): QuerySeq = filter(_.ref === ref)

  def findAllByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId)

  val returningLens: Lens[Capture, Int] = lens[Capture].id
}
