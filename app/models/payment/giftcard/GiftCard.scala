package models.payment.giftcard

import java.time.Instant

import cats.data.Validated._
import cats.data.{ValidatedNel, Xor}
import cats.implicits._
import com.pellucid.sealerate
import failures.GiftCardFailures._
import failures.{EmptyCancellationReasonFailure, Failure, Failures, GeneralFailure}
import models.StoreAdmin
import models.order.OrderPayment
import models.payment.PaymentMethod
import models.payment.giftcard.GiftCard._
import models.payment.giftcard.{GiftCardAdjustment ⇒ Adj, GiftCardAdjustments ⇒ Adjs}
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import shapeless._
import payloads.LineItemPayloads.AddGiftCardLineItem
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.http.CustomDirectives.SortAndPage
import utils.Money._
import utils.db._
import utils.Validation._
import utils._
import utils.aliases._
import utils.db._

case class GiftCard(id: Int = 0,
                    originId: Int,
                    originType: OriginType = CustomerPurchase,
                    code: String = "",
                    subTypeId: Option[Int] = None,
                    currency: Currency = Currency.USD,
                    state: State = GiftCard.Active,
                    originalBalance: Int,
                    currentBalance: Int = 0,
                    availableBalance: Int = 0,
                    canceledAmount: Option[Int] = None,
                    canceledReason: Option[Int] = None,
                    reloadable: Boolean = false,
                    customerId: Option[Int] = None,
                    createdAt: Instant = Instant.now())
    extends PaymentMethod
    with FoxModel[GiftCard]
    with FSM[GiftCard.State, GiftCard]
    with Validation[GiftCard] {

  import GiftCard._
  import Validation._

  override def validate: ValidatedNel[Failure, GiftCard] = {
    val canceledWithReason: ValidatedNel[Failure, Unit] =
      (state, canceledAmount, canceledReason) match {
        case (Canceled, None, _) ⇒
          invalidNel(GeneralFailure("canceledAmount must be present when canceled"))
        case (Canceled, _, None) ⇒
          invalidNel(GeneralFailure("canceledReason must be present when canceled"))
        case _ ⇒ valid({})
      }

    (canceledWithReason
        // |@| notEmpty(code, "code") // FIXME: this check does not allow to create models
        |@| validExpr(originalBalance >= 0, "originalBalance should be greater or equal than zero") |@| validExpr(
            currentBalance >= 0, "currentBalance should be greater or equal than zero")).map {
      case _ ⇒ this
    }
  }

  def stateLens                         = lens[GiftCard].state
  override def primarySearchKey: String = code
  override def updateTo(newModel: GiftCard): Failures Xor GiftCard =
    super.transitionModel(newModel)

  val fsm: Map[State, Set[State]] = Map(
      OnHold → Set(Active, Canceled),
      Active → Set(OnHold, Canceled),
      Cart   → Set(Canceled)
  )

  def isActive: Boolean = state == Active
  def isCart: Boolean   = state == Cart

  def hasAvailable(amount: Int): Boolean = availableBalance >= amount

  def mustBeCart: Failures Xor GiftCard =
    if (isCart) Xor.Right(this) else Xor.Left(GiftCardMustBeCart(code).single)

  def mustNotBeCart: Failures Xor GiftCard =
    if (!isCart) Xor.Right(this) else Xor.Left(GiftCardMustNotBeCart(code).single)

  def mustBeActive: Failures Xor GiftCard =
    if (isActive) Xor.Right(this) else Xor.Left(GiftCardIsInactive(this).single)

  def mustHaveEnoughBalance(amount: Int): Failures Xor GiftCard =
    if (hasAvailable(amount)) Xor.Right(this)
    else Xor.Left(GiftCardNotEnoughBalance(this, amount).single)
}

object GiftCard {
  sealed trait State
  case object OnHold        extends State
  case object Active        extends State
  case object Canceled      extends State
  case object Cart          extends State
  case object FullyRedeemed extends State

  sealed trait OriginType
  case object CsrAppeasement   extends OriginType
  case object CustomerPurchase extends OriginType
  case object FromStoreCredit  extends OriginType
  case object RmaProcess       extends OriginType

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  val giftCardCodeRegex = """([a-zA-Z0-9-_]*)""".r

  def update(giftCard: GiftCard, payload: AddGiftCardLineItem): GiftCard = {
    giftCard.copy(availableBalance = payload.balance,
                  currentBalance = payload.balance,
                  originalBalance = payload.balance,
                  currency = payload.currency)
  }

  def build(balance: Int, originId: Int, currency: Currency): GiftCard = {
    GiftCard(
        originId = originId,
        originType = GiftCard.CustomerPurchase,
        state = GiftCard.Active,
        currency = currency,
        originalBalance = balance,
        availableBalance = balance,
        currentBalance = balance
    )
  }

  def buildAppeasement(payload: GiftCardCreateByCsr, originId: Int): GiftCard = {
    GiftCard(
        originId = originId,
        originType = GiftCard.CsrAppeasement,
        subTypeId = payload.subTypeId,
        state = GiftCard.Active,
        currency = payload.currency,
        originalBalance = payload.balance,
        availableBalance = payload.balance,
        currentBalance = payload.balance
    )
  }

  def buildScTransfer(balance: Int, originId: Int, currency: Currency): GiftCard = {
    GiftCard(
        originId = originId,
        originType = GiftCard.FromStoreCredit,
        state = GiftCard.Active,
        currency = currency,
        originalBalance = balance,
        availableBalance = balance,
        currentBalance = balance
    )
  }

  def buildLineItem(balance: Int, originId: Int, currency: Currency): GiftCard = {
    GiftCard(
        originId = originId,
        originType = GiftCard.CustomerPurchase,
        state = GiftCard.Cart,
        currency = currency,
        originalBalance = balance,
        availableBalance = balance,
        currentBalance = balance
    )
  }

  def buildRmaProcess(originId: Int, currency: Currency): GiftCard = {
    GiftCard(
        originId = originId,
        originType = GiftCard.RmaProcess,
        state = GiftCard.Cart,
        currency = currency,
        originalBalance = 0,
        availableBalance = 0,
        currentBalance = 0
    )
  }

  def validateStateReason(state: State, reason: Option[Int]): ValidatedNel[Failure, Unit] = {
    if (state == Canceled) {
      validExpr(reason.isDefined, EmptyCancellationReasonFailure.description)
    } else {
      valid({})
    }
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] =
    OriginType.slickColumn
}

class GiftCards(tag: Tag) extends FoxTable[GiftCard](tag, "gift_cards") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId         = column[Int]("origin_id")
  def originType       = column[GiftCard.OriginType]("origin_type")
  def subTypeId        = column[Option[Int]]("subtype_id")
  def code             = column[String]("code")
  def state            = column[GiftCard.State]("state")
  def currency         = column[Currency]("currency")
  def originalBalance  = column[Int]("original_balance")
  def currentBalance   = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def canceledAmount   = column[Option[Int]]("canceled_amount")
  def canceledReason   = column[Option[Int]]("canceled_reason")
  def reloadable       = column[Boolean]("reloadable")
  def customerId       = column[Option[Int]]("customer_id")
  def createdAt        = column[Instant]("created_at")

  def * =
    (id,
     originId,
     originType,
     code,
     subTypeId,
     currency,
     state,
     originalBalance,
     currentBalance,
     availableBalance,
     canceledAmount,
     canceledReason,
     reloadable,
     customerId,
     createdAt) <> ((GiftCard.apply _).tupled, GiftCard.unapply)
}

object GiftCards
    extends FoxTableQuery[GiftCard, GiftCards](new GiftCards(_))
    with SearchByCode[GiftCard, GiftCards] {

  import GiftCard._

  def sortedAndPaged(query: QuerySeq)(implicit sortAndPage: SortAndPage): QuerySeqWithMetadata = {
    query.withMetadata.sortAndPageIfNeeded { (s, giftCard) ⇒
      s.sortColumn match {
        case "id"         ⇒ if (s.asc) giftCard.id.asc else giftCard.id.desc
        case "originId"   ⇒ if (s.asc) giftCard.originId.asc else giftCard.originId.desc
        case "originType" ⇒ if (s.asc) giftCard.originType.asc else giftCard.originType.desc
        case "subTypeId"  ⇒ if (s.asc) giftCard.subTypeId.asc else giftCard.subTypeId.desc
        case "code"       ⇒ if (s.asc) giftCard.code.asc else giftCard.code.desc
        case "state"      ⇒ if (s.asc) giftCard.state.asc else giftCard.state.desc
        case "currency"   ⇒ if (s.asc) giftCard.currency.asc else giftCard.currency.desc
        case "originalBalance" ⇒
          if (s.asc) giftCard.originalBalance.asc else giftCard.originalBalance.desc
        case "currentBalance" ⇒
          if (s.asc) giftCard.currentBalance.asc else giftCard.currentBalance.desc
        case "availableBalance" ⇒
          if (s.asc) giftCard.availableBalance.asc else giftCard.availableBalance.desc
        case "canceledAmount" ⇒
          if (s.asc) giftCard.canceledAmount.asc else giftCard.canceledAmount.desc
        case "canceledReason" ⇒
          if (s.asc) giftCard.canceledReason.asc else giftCard.canceledReason.desc
        case "reloadable" ⇒ if (s.asc) giftCard.reloadable.asc else giftCard.reloadable.desc
        case "customerId" ⇒ if (s.asc) giftCard.customerId.asc else giftCard.customerId.desc
        case "createdAt"  ⇒ if (s.asc) giftCard.createdAt.asc else giftCard.createdAt.desc
        case other        ⇒ invalidSortColumn(other)
      }
    }
  }

  def queryAll(implicit sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(this)

  def queryByCode(code: String)(implicit sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(findByCode(code))

  def auth(giftCard: GiftCard, orderPaymentId: Option[Int], debit: Int = 0, credit: Int = 0)(
      implicit ec: EC): DbResult[GiftCardAdjustment] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, state = Adj.Auth)

  def authOrderPayment(giftCard: GiftCard, pmt: OrderPayment)(
      implicit ec: EC): DbResult[GiftCardAdjustment] =
    auth(giftCard = giftCard, orderPaymentId = pmt.id.some, debit = pmt.amount.getOrElse(0))

  def capture(giftCard: GiftCard, orderPaymentId: Option[Int], debit: Int = 0, credit: Int = 0)(
      implicit ec: EC): DbResult[GiftCardAdjustment] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, state = Adj.Capture)

  def cancelByCsr(giftCard: GiftCard, storeAdmin: StoreAdmin)(
      implicit ec: EC): DbResult[GiftCardAdjustment] = {
    val adjustment = Adj(giftCardId = giftCard.id,
                         orderPaymentId = None,
                         storeAdminId = storeAdmin.id.some,
                         debit = giftCard.availableBalance,
                         credit = 0,
                         availableBalance = 0,
                         state = Adj.CancellationCapture)
    Adjs.create(adjustment)
  }

  def redeemToStoreCredit(giftCard: GiftCard, storeAdmin: StoreAdmin)(
      implicit ec: EC): DbResult[GiftCardAdjustment] = {
    val adjustment = Adj(giftCardId = giftCard.id,
                         orderPaymentId = None,
                         storeAdminId = storeAdmin.id.some,
                         debit = giftCard.availableBalance,
                         credit = 0,
                         availableBalance = 0,
                         state = Adj.Capture)
    Adjs.create(adjustment)
  }

  def findByCode(code: String): QuerySeq =
    filter(_.code === code)

  def findOneByCode(code: String): DBIO[Option[GiftCard]] =
    findByCode(code).one

  def findActiveByCode(code: String): QuerySeq =
    findByCode(code).filter(_.state === (GiftCard.Active: GiftCard.State))

  private def adjust(giftCard: GiftCard,
                     orderPaymentId: Option[Int],
                     debit: Int = 0,
                     credit: Int = 0,
                     state: GiftCardAdjustment.State = Adj.Auth)(
      implicit ec: EC): DbResult[GiftCardAdjustment] = {
    val balance = giftCard.availableBalance - debit + credit
    val adjustment = Adj(giftCardId = giftCard.id,
                         orderPaymentId = orderPaymentId,
                         debit = debit,
                         credit = credit,
                         availableBalance = balance,
                         state = state)
    Adjs.create(adjustment)
  }

  type Ret       = (Int, String, Int, Int)
  type PackedRet = (Rep[Int], Rep[String], Rep[Int], Rep[Int])
  override val returningQuery = map { gc ⇒
    (gc.id, gc.code, gc.currentBalance, gc.availableBalance)
  }
  private val rootLens = lens[GiftCard]
  val returningLens: Lens[GiftCard, (Int, String, Int, Int)] =
    rootLens.id ~ rootLens.code ~ rootLens.currentBalance ~ rootLens.availableBalance
}
