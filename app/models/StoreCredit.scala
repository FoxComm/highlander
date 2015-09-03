package models

import com.pellucid.sealerate
import services.{Result, Failures, Failure, OrderTotaler}
import slick.dbio.Effect.Read
import slick.profile.SqlAction
import utils.Money._
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}
import scala.concurrent.{ExecutionContext, Future}

import cats.data.ValidatedNel
import cats.implicits._
import utils.Litterbox._
import utils.Checks

import com.github.tototoshi.slick.JdbcJodaSupport._
import cats.data.Validated.{invalidNel, valid}
import com.pellucid.sealerate
import models.StoreCredit.{OnHold, Status}
import monocle.macros.GenLens
import org.joda.time.DateTime

import services.Failures
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Joda._
import utils.Money._
import utils.{ADT, NewModel, FSM, GenericTable, Model, ModelWithIdParameter, RichTable, TableQueryWithId}

final case class StoreCredit(id: Int = 0, customerId: Int, originId: Int, originType: String, currency: Currency,
  originalBalance: Int, currentBalance: Int = 0, availableBalance:Int = 0,
  status: Status = OnHold, canceledReason: Option[String] = None, createdAt: DateTime = DateTime.now())
  extends PaymentMethod
  with ModelWithIdParameter
  with FSM[StoreCredit.Status, StoreCredit]
  with NewModel {

  import StoreCredit._

  def isNew: Boolean = id == 0

  def validateNew: ValidatedNel[String, Model] = {
    val canceledWithReason = (status, canceledReason) match {
      case (Canceled, None) ⇒ invalidNel("canceledReason must be present when canceled")
      case _                ⇒ valid({})
    }

    (canceledWithReason
      |@| Checks.invalidExpr(originalBalance < currentBalance, "originalBalance cannot be less than currentBalance")
      |@| Checks.invalidExpr(originalBalance < availableBalance, "originalBalance cannot be less than availableBalance")
      |@| Checks.invalidExpr(originalBalance < 0, "originalBalance must be greater than zero")
    ).map { case _ ⇒ this }
  }

  def stateLens = GenLens[StoreCredit](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled)
  )

  // TODO: not sure we use this polymorphically
  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String] =
    Result.good("authenticated")

  def isActive: Boolean = activeStatuses.contains(status)
}

object StoreCredit {
  sealed trait Status
  case object OnHold extends Status
  case object Active extends Status
  case object Canceled extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  val activeStatuses = Set[Status](Active)

  implicit val statusColumnType = Status.slickColumn

  def processFifo(storeCredits: List[StoreCredit], requestedAmount: Int): Map[StoreCredit, Int] = {
    val fifo = storeCredits.sortBy(_.createdAt)
    fifo.foldLeft(Map.empty[StoreCredit, Int]) { case (amounts, sc) ⇒
      val total = amounts.values.sum
      val missing = requestedAmount - total

      if (total < requestedAmount) {
        if ((total + sc.availableBalance) >= requestedAmount) {
          amounts.updated(sc, missing)
        } else {
          amounts.updated(sc, sc.availableBalance)
        }
      } else {
        amounts
      }
    }
  }
}

class StoreCredits(tag: Tag) extends GenericTable.TableWithId[StoreCredit](tag, "store_credits") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId = column[Int]("origin_id")
  def originType = column[String]("origin_type")
  def customerId = column[Int]("customer_id")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def status = column[StoreCredit.Status]("status")
  def canceledReason = column[Option[String]]("canceled_reason")
  def createdAt = column[DateTime]("created_at")

  def * = (id, customerId, originId, originType, currency, originalBalance, currentBalance,
    availableBalance, status, canceledReason, createdAt) <> ((StoreCredit.apply _).tupled, StoreCredit.unapply)
}

object StoreCredits extends TableQueryWithId[StoreCredit, StoreCredits](
  idLens = GenLens[StoreCredit](_.id)
  )(new StoreCredits(_)){

  import StoreCredit._
  import models.{StoreCreditAdjustment ⇒ Adj, StoreCreditAdjustments ⇒ Adjs}

  def auth(storeCredit: StoreCredit, orderPaymentId: Int, amount: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, status = Adj.Auth)

  def capture(storeCredit: StoreCredit, orderPaymentId: Int, amount: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, status = Adj.Capture)

  def findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext, db: Database): Future[Seq[StoreCredit]] =
    _findAllByCustomerId(customerId).run()

  def _findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext): DBIO[Seq[StoreCredit]] =
    filter(_.customerId === customerId).result

  def findAllActiveByCustomerId(customerId: Int): Query[StoreCredits, StoreCredit, Seq] =
    filter(_.customerId === customerId).filter(_.status === (Active: Status)).filter(_.availableBalance > 0)

  def findByIdAndCustomerId(id: Int, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Option[StoreCredit]] =
    _findByIdAndCustomerId(id, customerId).run()

  def _findByIdAndCustomerId(id: Int, customerId: Int)(implicit ec: ExecutionContext): DBIO[Option[StoreCredit]] =
    filter(_.customerId === customerId).filter(_.id === id).take(1).result.headOption

  private def debit(storeCredit: StoreCredit, orderPaymentId: Int, amount: Int = 0,
    status: Adj.Status = Adj.Auth)
    (implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(storeCreditId = storeCredit.id, orderPaymentId = orderPaymentId,
      debit = amount, status = status)
    Adjs.save(adjustment)
  }
}
