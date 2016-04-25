package models.payment.storecredit

import java.time.Instant

import cats.data.Validated._
import cats.data.{ValidatedNel, Xor}
import cats.implicits._
import com.pellucid.sealerate
import models.StoreAdmin
import models.order.OrderPayment
import models.payment.PaymentMethod
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit._
import models.payment.storecredit.{StoreCreditAdjustment ⇒ Adj, StoreCreditAdjustments ⇒ Adjs}
import monocle.macros.GenLens
import failures.{Failure, Failures, GeneralFailure}
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.http.CustomDirectives.SortAndPage
import utils.Money._
import utils.Validation._
import utils.{Validation, _}
import utils.aliases._
import utils.db.{FoxModel, _}

case class StoreCredit(id: Int = 0, customerId: Int, originId: Int, originType: OriginType = CsrAppeasement,
  subTypeId: Option[Int] = None, currency: Currency = Currency.USD, originalBalance: Int, currentBalance: Int = 0,
  availableBalance: Int = 0, state: State = Active, canceledAmount: Option[Int] = None,
  canceledReason: Option[Int] = None, createdAt: Instant = Instant.now())
  extends PaymentMethod
  with FoxModel[StoreCredit]
  with FSM[StoreCredit.State, StoreCredit]
  with Validation[StoreCredit] {

  override def validate: ValidatedNel[Failure, StoreCredit] = {
    val canceledWithReason: ValidatedNel[Failure, Unit] = (state, canceledAmount, canceledReason) match {
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

  def stateLens = GenLens[StoreCredit](_.state)
  override def updateTo(newModel: StoreCredit): Failures Xor StoreCredit = super.transitionModel(newModel)

  val fsm: Map[State, Set[State]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled)
  )

  def isActive: Boolean = state == Active
}

object StoreCredit {
  sealed trait State
  case object OnHold extends State
  case object Active extends State
  case object Canceled extends State
  case object FullyRedeemed extends State

  sealed trait OriginType
  case object CsrAppeasement extends OriginType
  case object GiftCardTransfer extends OriginType
  case object RmaProcess extends OriginType
  case object Custom extends OriginType

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
    def publicTypes = types.--(Seq(Custom))
  }

  def validateStateReason(state: State, reason: Option[Int]): ValidatedNel[Failure, Unit] = {
    if (state == Canceled) {
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

  def buildFromExtension(customerId: Int,
    payload: payloads.CreateExtensionStoreCredit,
    originType: StoreCredit.OriginType = StoreCredit.Custom,
    originId: Int): StoreCredit = {
    StoreCredit(customerId = customerId,
      originType = originType,
      originId = originId,
      currency = payload.currency,
      subTypeId = payload.subTypeId,
      originalBalance = payload.amount)
  }

  def buildRmaProcess(customerId: Int, originId: Int, currency: Currency): StoreCredit = {
    StoreCredit(customerId = customerId, originId = originId, originType = StoreCredit.RmaProcess,
      currency = currency, originalBalance = 0)
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
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

class StoreCredits(tag: Tag) extends FoxTable[StoreCredit](tag, "store_credits")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId = column[Int]("origin_id")
  def originType = column[StoreCredit.OriginType]("origin_type")
  def subTypeId = column[Option[Int]]("subtype_id")
  def customerId = column[Int]("customer_id")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def state = column[StoreCredit.State]("state")
  def canceledAmount = column[Option[Int]]("canceled_amount")
  def canceledReason = column[Option[Int]]("canceled_reason")
  def createdAt = column[Instant]("created_at")

  def * = (id, customerId, originId, originType, subTypeId, currency, originalBalance, currentBalance,
    availableBalance, state, canceledAmount, canceledReason, createdAt) <> ((StoreCredit.apply _).tupled, StoreCredit
    .unapply)
}

object StoreCredits extends FoxTableQuery[StoreCredit, StoreCredits](
  idLens = GenLens[StoreCredit](_.id)
  )(new StoreCredits(_)){

  def sortedAndPaged(query: QuerySeq)(implicit sortAndPage: SortAndPage): QuerySeqWithMetadata =
    query.withMetadata.sortAndPageIfNeeded { case (s, storeCredit) ⇒
      s.sortColumn match {
        case "id"               ⇒ if (s.asc) storeCredit.id.asc               else storeCredit.id.desc
        case "originId"         ⇒ if (s.asc) storeCredit.originId.asc         else storeCredit.originId.desc
        case "originType"       ⇒ if (s.asc) storeCredit.originType.asc       else storeCredit.originType.desc
        case "state"            ⇒ if (s.asc) storeCredit.state.asc            else storeCredit.state.desc
        case "customerId"       ⇒ if (s.asc) storeCredit.customerId.asc       else storeCredit.customerId.desc
        case "currency"         ⇒ if (s.asc) storeCredit.currency.asc         else storeCredit.currency.desc
        case "originalBalance"  ⇒ if (s.asc) storeCredit.originalBalance.asc  else storeCredit.originalBalance.desc
        case "currentBalance"   ⇒ if (s.asc) storeCredit.currentBalance.asc   else storeCredit.currentBalance.desc
        case "availableBalance" ⇒ if (s.asc) storeCredit.availableBalance.asc else storeCredit.availableBalance.desc
        case "canceledAmount"   ⇒ if (s.asc) storeCredit.canceledAmount.asc   else storeCredit.canceledAmount.desc
        case "canceledReason"   ⇒ if (s.asc) storeCredit.canceledReason.asc   else storeCredit.canceledReason.desc
        case "createdAt"        ⇒ if (s.asc) storeCredit.createdAt.asc        else storeCredit.createdAt.desc
        case other              ⇒ invalidSortColumn(other)
      }
    }

  def queryByCustomer(customerId: Int)(implicit sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(findAllByCustomerId(customerId))

  def auth(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0)
    (implicit ec: EC): DbResult[StoreCreditAdjustment] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, state = Adj.Auth)

  def authOrderPayment(storeCredit: StoreCredit, pmt: OrderPayment)(implicit ec: EC): DbResult[StoreCreditAdjustment] =
    auth(storeCredit = storeCredit, orderPaymentId = pmt.id.some, amount = pmt.amount.getOrElse(0))

  def capture(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0)
    (implicit ec: EC): DbResult[StoreCreditAdjustment] =
    debit(storeCredit = storeCredit, orderPaymentId = orderPaymentId, amount = amount, state = Adj.Capture)

  def cancelByCsr(storeCredit: StoreCredit, storeAdmin: StoreAdmin)(implicit ec: EC): DbResult[StoreCreditAdjustment] = {
    val adjustment = Adj(storeCreditId = storeCredit.id, orderPaymentId = None, storeAdminId = storeAdmin.id.some,
      debit = storeCredit.availableBalance, availableBalance = 0, state = Adj.CancellationCapture)
    Adjs.create(adjustment)
  }

  def redeemToGiftCard(storeCredit: StoreCredit, storeAdmin: StoreAdmin)(implicit ec: EC): DbResult[StoreCreditAdjustment] = {
    val adjustment = Adj(storeCreditId = storeCredit.id, orderPaymentId = None, storeAdminId = storeAdmin.id.some,
      debit = storeCredit.availableBalance, availableBalance = 0, state = Adj.Capture)
    Adjs.create(adjustment)
  }

  def findActiveById(id: Int): QuerySeq = filter(_.id === id)

  def findAllByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId)

  def findAllActiveByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId).filter(_.state === (Active: State)).filter(_.availableBalance > 0)

  def findByIdAndCustomerId(id: Int, customerId: Int): DBIO[Option[StoreCredit]] =
    filter(_.customerId === customerId).filter(_.id === id).one

  type ReturningIdAndBalances = (Int, Int, Int)

  val returningIdAndBalances: Returning[ReturningIdAndBalances] =
    this.returning(map { sc ⇒ (sc.id, sc.currentBalance, sc.availableBalance) })

  def returningAction(ret: ReturningIdAndBalances)(gc: StoreCredit): StoreCredit = ret match {
    case (id, currentBalance, availableBalance) ⇒
      gc.copy(id = id, currentBalance = currentBalance, availableBalance = availableBalance)
  }

  override def create[R](sc: StoreCredit, returning: Returning[R], action: R ⇒ StoreCredit ⇒ StoreCredit)
    (implicit ec: EC): DbResult[StoreCredit] =
    super.create(sc, returningIdAndBalances, returningAction)

  private def debit(storeCredit: StoreCredit, orderPaymentId: Option[Int], amount: Int = 0,
    state: StoreCreditAdjustment.State = Adj.Auth)
    (implicit ec: EC): DbResult[StoreCreditAdjustment] = {
    val adjustment = Adj(storeCreditId = storeCredit.id, orderPaymentId = orderPaymentId,
      debit = amount, availableBalance = storeCredit.availableBalance, state = state)
    Adjs.create(adjustment)
  }
}
