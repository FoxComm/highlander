package models

import java.time.Instant

import scala.concurrent.ExecutionContext

import cats.data.Validated._
import cats.data.ValidatedNel
import cats.implicits._
import services._
import utils.CustomDirectives.SortAndPage
import utils.Litterbox._
import utils.Validation

import com.pellucid.sealerate
import models.StoreCredit.{CsrAppeasement, Active, Status, OriginType}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType

import utils._
import utils.Money._
import utils.Slick.implicits._
import utils.Validation._

final case class StoreCredit(id: Int = 0, customerId: Int, originId: Int, originType: OriginType = CsrAppeasement,
  subTypeId: Option[Int] = None, currency: Currency = Currency.USD, originalBalance: Int, currentBalance: Int = 0,
  availableBalance: Int = 0, status: Status = Active, canceledAmount: Option[Int] = None,
  canceledReason: Option[Int] = None, createdAt: Instant = Instant.now())
  extends PaymentMethod
  with ModelWithIdParameter[StoreCredit]
  with FSM[StoreCredit.Status, StoreCredit]
  with Validation[StoreCredit] {

  import StoreCredit._
  import Validation._

  override def validate: ValidatedNel[Failure, StoreCredit] = {
    val canceledWithReason: ValidatedNel[Failure, Unit] = (status, canceledAmount, canceledReason) match {
      case (Canceled, None, _) ⇒ invalidNel(GeneralFailure("canceledAmount must be present when canceled"))
      case (Canceled, _, None) ⇒ invalidNel(GeneralFailure("canceledReason must be present when canceled"))
      case _                   ⇒ valid({})
    }

    (canceledWithReason
      |@| invalidExpr(originalBalance < currentBalance, "originalBalance cannot be less than currentBalance")
      |@| invalidExpr(originalBalance < availableBalance, "originalBalance cannot be less than availableBalance")
      |@| invalidExpr(originalBalance < 0, "originalBalance must be greater than zero")
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
  case object FullyRedeemed extends Status

  sealed trait OriginType
  case object CsrAppeasement extends OriginType
  case object GiftCardTransfer extends OriginType
  case object ReturnProcess extends OriginType

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  val activeStatuses = Set[Status](Active)

  def validateStatusReason(status: Status, reason: Option[Int]): ValidatedNel[Failure, Unit] = {
    if (status == Canceled) {
      validExpr(reason.isDefined, "Please provide valid cancellation reason")
    } else {
      valid({})
    }
  }

  def buildFromGcTransfer(customerId: Int, gc: GiftCard): StoreCredit = {
    StoreCredit(customerId = customerId, originId = 0, originType = StoreCredit.GiftCardTransfer,
      currency = gc.currency, originalBalance = gc.currentBalance, currentBalance = gc.currentBalance)
  }

  def buildAppeasement(customerId: Int, originId: Int, payload: payloads.CreateManualStoreCredit): StoreCredit = {
    StoreCredit(customerId = customerId, originId = originId, originType = StoreCredit.CsrAppeasement,
      subTypeId = payload.subTypeId, currency = payload.currency, originalBalance = payload.amount)
  }

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn

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

class StoreCredits(tag: Tag) extends GenericTable.TableWithId[StoreCredit](tag, "store_credits")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId = column[Int]("origin_id")
  def originType = column[StoreCredit.OriginType]("origin_type")
  def subTypeId = column[Option[Int]]("subtype_id")
  def customerId = column[Int]("customer_id")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def status = column[StoreCredit.Status]("status")
  def canceledAmount = column[Option[Int]]("canceled_amount")
  def canceledReason = column[Option[Int]]("canceled_reason")
  def createdAt = column[Instant]("created_at")

  def * = (id, customerId, originId, originType, subTypeId, currency, originalBalance, currentBalance,
    availableBalance, status, canceledAmount, canceledReason, createdAt) <> ((StoreCredit.apply _).tupled, StoreCredit
    .unapply)
}

object StoreCredits extends TableQueryWithId[StoreCredit, StoreCredits](
  idLens = GenLens[StoreCredit](_.id)
  )(new StoreCredits(_)){

  import StoreCredit._
  import models.{StoreCreditAdjustment ⇒ Adj, StoreCreditAdjustments ⇒ Adjs}

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    query.withMetadata.sortAndPageIfNeeded { case (s, storeCredit) ⇒
      s.sortColumn match {
        case "id"               ⇒ if(s.asc) storeCredit.id.asc               else storeCredit.id.desc
        case "originId"         ⇒ if(s.asc) storeCredit.originId.asc         else storeCredit.originId.desc
        case "originType"       ⇒ if(s.asc) storeCredit.originType.asc       else storeCredit.originType.desc
        case "customerId"       ⇒ if(s.asc) storeCredit.customerId.asc       else storeCredit.customerId.desc
        case "currency"         ⇒ if(s.asc) storeCredit.currency.asc         else storeCredit.currency.desc
        case "originalBalance"  ⇒ if(s.asc) storeCredit.originalBalance.asc  else storeCredit.originalBalance.desc
        case "currentBalance"   ⇒ if(s.asc) storeCredit.currentBalance.asc   else storeCredit.currentBalance.desc
        case "availableBalance" ⇒ if(s.asc) storeCredit.availableBalance.asc else storeCredit.availableBalance.desc
        case "canceledAmount"   ⇒ if(s.asc) storeCredit.canceledAmount.asc   else storeCredit.canceledAmount.desc
        case "canceledReason"   ⇒ if(s.asc) storeCredit.canceledReason.asc   else storeCredit.canceledReason.desc
        case "createdAt"        ⇒ if(s.asc) storeCredit.createdAt.asc      else storeCredit.createdAt.desc
        case other              ⇒ invalidSortColumn(other)
      }
    }

  def queryByCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(findAllByCustomerId(customerId))

  def auth(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, status = Adj.Auth)

  def capture(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, status = Adj.Capture)

  def cancelByCsr(storeCredit: StoreCredit, storeAdmin: StoreAdmin)(implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(storeCreditId = storeCredit.id, orderPaymentId = None, storeAdminId = storeAdmin.id.some,
      debit = storeCredit.availableBalance, availableBalance = 0, status = Adj.CancellationCapture)
    Adjs.save(adjustment)
  }

  def redeemToGiftCard(storeCredit: StoreCredit, storeAdmin: StoreAdmin)(implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(storeCreditId = storeCredit.id, orderPaymentId = None, storeAdminId = storeAdmin.id.some,
      debit = storeCredit.availableBalance, availableBalance = 0, status = Adj.Capture)
    Adjs.save(adjustment)
  }

  def findActiveById(id: Int)(implicit ec: ExecutionContext): QuerySeq = filter(_.id === id)

  def findAllByCustomerId(customerId: Int)(implicit ec: ExecutionContext): QuerySeq =
    filter(_.customerId === customerId)

  def findAllActiveByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId).filter(_.status === (Active: Status)).filter(_.availableBalance > 0)

  def findByIdAndCustomerId(id: Int, customerId: Int)(implicit ec: ExecutionContext): DBIO[Option[StoreCredit]] =
    filter(_.customerId === customerId).filter(_.id === id).one

  private def debit(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0,
    status: Adj.Status = Adj.Auth)
    (implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(storeCreditId = storeCredit.id, orderPaymentId = orderPaymentId,
      debit = amount, availableBalance = storeCredit.availableBalance, status = status)
    Adjs.save(adjustment)
  }
}
