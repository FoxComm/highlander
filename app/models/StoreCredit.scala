package models

import com.pellucid.sealerate
import services.{Failure, OrderTotaler}
import slick.dbio.Effect.Read
import slick.profile.SqlAction
import utils.Money._
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}
import validators.nonEmptyIf

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class StoreCredit(id: Int = 0, customerId: Int, originId: Int, originType: String, currency: Currency,
  originalBalance: Int, currentBalance: Int, status: StoreCredit.Status = StoreCredit.New,
  canceledReason: Option[String] = None)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[StoreCredit] {

  import StoreCredit._

  override def validator = createValidator[StoreCredit] { storeCredit =>
    storeCredit.status as "canceledReason" is nonEmptyIf(storeCredit.status == Canceled, storeCredit.canceledReason)
  }

  // TODO: not sure we use this polymorphically
  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[Failure]] = {
    Future.successful(Good("authenticated"))
  }

  def isActive: Boolean = activeStatuses.contains(status)
}

object StoreCredit {
  sealed trait Status
  case object New extends Status
  case object Auth extends Status
  case object Hold extends Status
  case object Canceled extends Status
  case object PartiallyApplied extends Status
  case object Applied extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  val activeStatuses = Set[Status](New, Auth, PartiallyApplied)

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
  def status = column[StoreCredit.Status]("status")
  def canceledReason = column[Option[String]]("canceled_reason")
  def * = (id, customerId, originId, originType, currency, originalBalance, currentBalance,
    status, canceledReason) <> ((StoreCredit.apply _).tupled, StoreCredit.unapply)
}

object StoreCredits extends TableQueryWithId[StoreCredit, StoreCredits](
  idLens = GenLens[StoreCredit](_.id)
  )(new StoreCredits(_)){

  def debit(storeCredit: StoreCredit, debit: Int = 0, capture: Boolean)
    (implicit ec: ExecutionContext): DBIO[StoreCreditAdjustment] = {
    val adjustment = StoreCreditAdjustment(storeCreditId = storeCredit.id, debit = debit, capture = capture)
    StoreCreditAdjustments.save(adjustment)
  }

  override def save(storeCredit: StoreCredit)(implicit ec: ExecutionContext): DBIO[StoreCredit] = for {
    id ← returningId += storeCredit.copy(currentBalance = storeCredit.originalBalance)
  } yield storeCredit.copy(id = id)

  def findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext, db: Database): Future[Seq[StoreCredit]] =
    _findAllByCustomerId(customerId).run()

  def _findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext): DBIO[Seq[StoreCredit]] =
    filter(_.customerId === customerId).result

  def findByIdAndCustomerId(id: Int, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Option[StoreCredit]] =
    _findByIdAndCustomerId(id, customerId).run()

  def _findByIdAndCustomerId(id: Int, customerId: Int)(implicit ec: ExecutionContext): DBIO[Option[StoreCredit]] =
    filter(_.customerId === customerId).filter(_.id === id).take(1).result.headOption
}
