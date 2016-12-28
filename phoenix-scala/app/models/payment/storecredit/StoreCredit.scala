package models.payment.storecredit

import java.time.Instant

import cats.data.Validated._
import cats.data.{ValidatedNel, Xor}
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import failures.{Failure, Failures, GeneralFailure, StoreCreditFailures}
import models.account._
import models.cord.OrderPayment
import models.payment.PaymentMethod
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit._
import models.payment.storecredit.{StoreCreditAdjustment ⇒ Adj, StoreCreditAdjustments ⇒ Adjs}
import payloads.PaymentPayloads._
import shapeless._
import slick.ast.BaseTypedType
import utils.db.ExPostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Money._
import utils.Validation._
import utils._
import utils.aliases._
import utils.db._

case class StoreCredit(id: Int = 0,
                       scope: LTree,
                       accountId: Int,
                       originId: Int,
                       originType: OriginType = CsrAppeasement,
                       subTypeId: Option[Int] = None,
                       currency: Currency = Currency.USD,
                       originalBalance: Int,
                       currentBalance: Int = 0,
                       availableBalance: Int = 0,
                       state: State = Active,
                       canceledAmount: Option[Int] = None,
                       canceledReason: Option[Int] = None,
                       createdAt: Instant = Instant.now())
    extends PaymentMethod
    with FoxModel[StoreCredit]
    with FSM[StoreCredit.State, StoreCredit]
    with Validation[StoreCredit] {

  override def validate: ValidatedNel[Failure, StoreCredit] = {
    val canceledWithReason: ValidatedNel[Failure, Unit] =
      (state, canceledAmount, canceledReason) match {
        case (Canceled, None, _) ⇒
          invalidNel(GeneralFailure("canceledAmount must be present when canceled"))
        case (Canceled, _, None) ⇒
          invalidNel(GeneralFailure("canceledReason must be present when canceled"))
        case _ ⇒ valid({})
      }

    (canceledWithReason |@| invalidExpr(
            originalBalance < currentBalance,
            "originalBalance cannot be less than currentBalance") |@| invalidExpr(
            originalBalance < availableBalance,
            "originalBalance cannot be less than availableBalance") |@| invalidExpr(
            originalBalance < 0,
            "originalBalance must be greater than zero")).map {
      case _ ⇒ this
    }
  }

  def stateLens = lens[StoreCredit].state
  override def updateTo(newModel: StoreCredit): Failures Xor StoreCredit =
    super.transitionModel(newModel)

  val fsm: Map[State, Set[State]] = Map(
      OnHold → Set(Active, Canceled),
      Active → Set(OnHold, Canceled)
  )

  def isActive: Boolean = state == Active
}

object StoreCredit {
  sealed trait State
  case object OnHold        extends State
  case object Active        extends State
  case object Canceled      extends State
  case object FullyRedeemed extends State

  sealed trait OriginType
  case object CsrAppeasement   extends OriginType
  case object GiftCardTransfer extends OriginType
  case object RmaProcess       extends OriginType
  case object Custom           extends OriginType

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  object OriginType extends ADT[OriginType] {
    def types       = sealerate.values[OriginType]
    def publicTypes = types.--(Seq(Custom))
  }

  def validateStateReason(state: State, reason: Option[Int]): ValidatedNel[Failure, Unit] = {
    if (state == Canceled) {
      validExpr(reason.isDefined, "Please provide valid cancellation reason")
    } else {
      valid({})
    }
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] =
    OriginType.slickColumn

  def processFifo(storeCredits: List[StoreCredit], requestedAmount: Int): Map[StoreCredit, Int] = {
    val fifo = storeCredits.sortBy(_.createdAt)
    fifo.foldLeft(Map.empty[StoreCredit, Int]) {
      case (amounts, sc) ⇒
        val total   = amounts.values.sum
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

class StoreCredits(tag: Tag) extends FoxTable[StoreCredit](tag, "store_credits") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope            = column[LTree]("scope")
  def originId         = column[Int]("origin_id")
  def originType       = column[StoreCredit.OriginType]("origin_type")
  def subTypeId        = column[Option[Int]]("subtype_id")
  def accountId        = column[Int]("account_id")
  def currency         = column[Currency]("currency")
  def originalBalance  = column[Int]("original_balance")
  def currentBalance   = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def state            = column[StoreCredit.State]("state")
  def canceledAmount   = column[Option[Int]]("canceled_amount")
  def canceledReason   = column[Option[Int]]("canceled_reason")
  def createdAt        = column[Instant]("created_at")

  def * =
    (id,
     scope,
     accountId,
     originId,
     originType,
     subTypeId,
     currency,
     originalBalance,
     currentBalance,
     availableBalance,
     state,
     canceledAmount,
     canceledReason,
     createdAt) <> ((StoreCredit.apply _).tupled, StoreCredit.unapply)
}

object StoreCredits extends FoxTableQuery[StoreCredit, StoreCredits](new StoreCredits(_)) {

  def auth(storeCredit: StoreCredit, orderPaymentId: Int, amount: Int = 0)(
      implicit ec: EC): DbResultT[StoreCreditAdjustment] =
    debit(storeCredit = storeCredit,
          orderPaymentId = orderPaymentId.some,
          amount = amount,
          state = Adj.Auth)

  def authOrderPayment(
      storeCredit: StoreCredit,
      pmt: OrderPayment,
      maxPaymentAmount: Option[Int] = None)(implicit ec: EC): DbResultT[StoreCreditAdjustment] =
    auth(storeCredit = storeCredit,
         orderPaymentId = pmt.id,
         amount = pmt.getAmount(maxPaymentAmount))

  def captureOrderPayment(
      storeCredit: StoreCredit,
      pmt: OrderPayment,
      maxPaymentAmount: Option[Int] = None)(implicit ec: EC): DbResultT[StoreCreditAdjustment] =
    capture(storeCredit = storeCredit,
            orderPaymentId = pmt.id,
            amount = pmt.getAmount(maxPaymentAmount))

  def capture(storeCredit: StoreCredit, orderPaymentId: Int, amount: Int)(
      implicit ec: EC): DbResultT[StoreCreditAdjustment] =
    for {
      auth ← * <~ StoreCreditAdjustments
              .authorizedOrderPayment(orderPaymentId)
              .mustFindOneOr(StoreCreditFailures.StoreCreditAuthAdjustmentNotFound(orderPaymentId))
      _ ← * <~ (
             require(amount <= auth.debit)
         )
      cap ← * <~ StoreCreditAdjustments.update(auth,
                                               auth.copy(debit = amount, state = Adj.Capture))
    } yield cap

  def cancelByCsr(storeCredit: StoreCredit, storeAdmin: User)(
      implicit ec: EC): DbResultT[StoreCreditAdjustment] = {
    val adjustment = Adj(storeCreditId = storeCredit.id,
                         orderPaymentId = None,
                         storeAdminId = storeAdmin.accountId.some,
                         debit = storeCredit.availableBalance,
                         availableBalance = 0,
                         state = Adj.CancellationCapture)
    Adjs.create(adjustment)
  }

  def redeemToGiftCard(storeCredit: StoreCredit, storeAdmin: User)(
      implicit ec: EC): DbResultT[StoreCreditAdjustment] = {
    val adjustment = Adj(storeCreditId = storeCredit.id,
                         orderPaymentId = None,
                         storeAdminId = storeAdmin.accountId.some,
                         debit = storeCredit.availableBalance,
                         availableBalance = 0,
                         state = Adj.Capture)
    Adjs.create(adjustment)
  }

  def findActiveById(id: Int): QuerySeq = filter(_.id === id)

  def findAllByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def findAllActiveByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)
      .filter(_.state === (Active: State))
      .filter(_.availableBalance > 0)

  def findActive(): QuerySeq =
    filter(_.state === (Active: State)).filter(_.availableBalance > 0)

  def findByIdAndAccountId(id: Int, accountId: Int): DBIO[Option[StoreCredit]] =
    filter(_.accountId === accountId).filter(_.id === id).one

  private def debit(storeCredit: StoreCredit,
                    orderPaymentId: Option[Int],
                    amount: Int = 0,
                    state: StoreCreditAdjustment.State = Adj.Auth)(
      implicit ec: EC): DbResultT[StoreCreditAdjustment] = {
    val adjustment = Adj(storeCreditId = storeCredit.id,
                         orderPaymentId = orderPaymentId,
                         debit = amount,
                         availableBalance = storeCredit.availableBalance,
                         state = state)
    Adjs.create(adjustment)
  }

  type Ret       = (Int, Int, Int)
  type PackedRet = (Rep[Int], Rep[Int], Rep[Int])
  private val rootLens = lens[StoreCredit]
  val returningLens: Lens[StoreCredit, (Int, Int, Int)] =
    rootLens.id ~ rootLens.currentBalance ~ rootLens.availableBalance
  val returningQuery = map { sc ⇒
    (sc.id, sc.currentBalance, sc.availableBalance)
  }
}
