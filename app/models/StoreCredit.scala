package models

import scala.concurrent.{ExecutionContext, Future}

import cats.data.NonEmptyList
import cats.implicits._
import utils.Litterbox._
import cats.data.Validated.{invalidNel, valid}
import com.pellucid.sealerate
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import models.StoreCredit.{OnHold, Status}
import monocle.macros.GenLens
import org.scalactic._
import services.Failures
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Money._
import utils.{Model, ADT, FSM, GenericTable, ModelWithIdParameter, RichTable, TableQueryWithId, Validation}
import validators.nonEmptyIf


trait NewModel extends Model {
  /* We could give back XorNel instead of ValidatedNel and String could be a Error/Failure type for model
     validation which would be distinct from our services.Failure type */
  type XorNel = cats.data.Xor[NonEmptyList[String], Model]
  type ValidatedNel = cats.data.ValidatedNel[String, Model]

  def isNew: Boolean

  def validateNew: ValidatedNel = cats.data.Validated.valid(this)
}

final case class StoreCredit(id: Int = 0, customerId: Int, originId: Int, originType: String, currency: Currency,
  originalBalance: Int, currentBalance: Int = 0, availableBalance:Int = 0,
  status: Status = OnHold, canceledReason: Option[String] = None)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[StoreCredit]
  with FSM[StoreCredit.Status, StoreCredit]
  with NewModel {

  import StoreCredit._

  // would make this default on ModelWithIdParameter
  def isNew: Boolean = id == 0

  override def validateNew: ValidatedNel = {
    def validate(isBad: Boolean, err: String) = if (isBad) invalidNel(err) else valid({})

    val canceledWithReason = (status, canceledReason) match {
      case (Canceled, None) ⇒ invalidNel("canceledReason must be present when canceled")
      case _                ⇒ valid({})
    }

    (canceledWithReason
      |@| validate(originalBalance < currentBalance, "originalBalance cannot be less than currentBalance")
      |@| validate(originalBalance < availableBalance, "originalBalance cannot be less than availableBalance")
      |@| validate(originalBalance <= 0, "originalBalance must be greater than zero")
    ).map { case _ ⇒ this }
  }

  // we'll drop this entirely
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

  def isActive: Boolean = status == Active
}

object StoreCredit {
  sealed trait Status
  case object OnHold extends Status
  case object Active extends Status
  case object Canceled extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

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

  import StoreCreditAdjustment.{Auth, Capture, Status}

  def auth(storeCredit: StoreCredit, orderPaymentId: Int, amount: Int = 0)
    (implicit ec: ExecutionContext): DBIO[StoreCreditAdjustment] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, status = Auth)

  def capture(storeCredit: StoreCredit, orderPaymentId: Int, amount: Int = 0)
    (implicit ec: ExecutionContext): DBIO[StoreCreditAdjustment] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, status = Capture)

  def findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext, db: Database): Future[Seq[StoreCredit]] =
    _findAllByCustomerId(customerId).run()

  def _findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext): DBIO[Seq[StoreCredit]] =
    filter(_.customerId === customerId).result

  def findByIdAndCustomerId(id: Int, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Option[StoreCredit]] =
    _findByIdAndCustomerId(id, customerId).run()

  def _findByIdAndCustomerId(id: Int, customerId: Int)(implicit ec: ExecutionContext): DBIO[Option[StoreCredit]] =
    filter(_.customerId === customerId).filter(_.id === id).take(1).result.headOption

  private def debit(storeCredit: StoreCredit, orderPaymentId: Int, amount: Int = 0, status: Status = Auth)
    (implicit ec: ExecutionContext): DBIO[StoreCreditAdjustment] = {
    val adjustment = StoreCreditAdjustment(storeCreditId = storeCredit.id, orderPaymentId = orderPaymentId,
      debit = amount, status = status)
    StoreCreditAdjustments.save(adjustment)
  }
}
