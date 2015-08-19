package models

import scala.concurrent.{ExecutionContext, Future}

import com.pellucid.sealerate
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import models.StoreCredit.{OnHold, Status}
import monocle.macros.GenLens
import org.scalactic._
import services.Failures
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Money._
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, RichTable, TableQueryWithId, Validation}
import validators.nonEmptyIf

final case class StoreCredit(id: Int = 0, customerId: Int, originId: Int, originType: String, currency: Currency,
  originalBalance: Int, currentBalance: Int = 0, availableBalance:Int = 0,
  status: Status = OnHold, canceledReason: Option[String] = None)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[StoreCredit]
  with FSM[StoreCredit.Status, StoreCredit] {

  import StoreCredit._

  override def validator = createValidator[StoreCredit] { storeCredit =>
    storeCredit.status as "canceledReason" is nonEmptyIf(storeCredit.status == Canceled, storeCredit
      .canceledReason)
  }

  def stateLens = GenLens[StoreCredit](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled)
  )

  // TODO: not sure we use this polymorphically
  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or Failures] = {
    Future.successful(Good("authenticated"))
  }

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
  def * = (id, customerId, originId, originType, currency, originalBalance, currentBalance,
    availableBalance, status, canceledReason) <> ((StoreCredit.apply _).tupled, StoreCredit.unapply)
}

object StoreCredits extends TableQueryWithId[StoreCredit, StoreCredits](
  idLens = GenLens[StoreCredit](_.id)
  )(new StoreCredits(_)){

  import models.{StoreCreditAdjustment ⇒ Adj, StoreCreditAdjustments ⇒ Adjs}
  import StoreCredit._

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
    filter(_.customerId === customerId).filter(_.status === (Active: Status))

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
