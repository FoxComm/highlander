package phoenix.models.payment.giftcard

import java.time.Instant

import cats.data.Validated._
import cats.data.ValidatedNel
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures._
import core.utils.Money._
import core.utils.Validation
import core.utils.Validation._
import phoenix.failures.EmptyCancellationReasonFailure
import phoenix.failures.GiftCardFailures._
import phoenix.models.account._
import phoenix.models.cord.OrderPayment
import phoenix.models.payment.giftcard.GiftCard._
import phoenix.models.payment.giftcard.{GiftCardAdjustment ⇒ Adj, GiftCardAdjustments ⇒ Adjs}
import phoenix.models.payment.{InStorePaymentStates, PaymentMethod}
import phoenix.payloads.GiftCardPayloads.{GiftCardCreateByCsr, GiftCardCreatedByCustomer}
import phoenix.utils.aliases._
import phoenix.utils.{ADT, FSM}
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

case class GiftCard(id: Int = 0,
                    scope: LTree,
                    originId: Int,
                    originType: OriginType = CustomerPurchase,
                    code: String = "",
                    subTypeId: Option[Int] = None,
                    currency: Currency = Currency.USD,
                    state: State = GiftCard.Active,
                    originalBalance: Long,
                    currentBalance: Long = 0, // opening balance minus ‘captured’ debits
                    availableBalance: Long = 0, // current balance minus ‘auth’ debits
                    canceledAmount: Option[Long] = None,
                    canceledReason: Option[Int] = None,
                    reloadable: Boolean = false,
                    accountId: Option[Int] = None,
                    createdAt: Instant = Instant.now(),
                    senderName: Option[String] = None,
                    recipientName: Option[String] = None,
                    recipientEmail: Option[String] = None,
                    message: Option[String] = None)
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
      currentBalance >= 0,
      "currentBalance should be greater or equal than zero")).map {
      case _ ⇒ this
    }
  }

  def stateLens                         = lens[GiftCard].state
  override def primarySearchKey: String = code
  override def updateTo(newModel: GiftCard): Either[Failures, GiftCard] =
    super.transitionModel(newModel)

  val fsm: Map[State, Set[State]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled),
    Cart   → Set(Canceled)
  )

  def isActive: Boolean = state == Active
  def isCart: Boolean   = state == Cart

  def mustBeCart: Either[Failures, GiftCard] =
    if (isCart) Either.right(this) else Either.left(GiftCardMustBeCart(code).single)

  def mustNotBeCart: Either[Failures, GiftCard] =
    if (!isCart) Either.right(this) else Either.left(GiftCardMustNotBeCart(code).single)

  def mustBeActive: Either[Failures, GiftCard] =
    if (isActive) Either.right(this) else Either.left(GiftCardIsInactive(this.code).single)

  def mustHaveEnoughBalance(amount: Long): Either[Failures, GiftCard] =
    if (availableBalance >= amount) Either.right(this)
    else Either.left(GiftCardNotEnoughBalance(this, amount).single)
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

  def build(balance: Long, originId: Int, currency: Currency)(implicit au: AU): GiftCard =
    GiftCard(
      scope = Scope.current,
      originId = originId,
      originType = GiftCard.CustomerPurchase,
      state = GiftCard.Active,
      currency = currency,
      originalBalance = balance,
      availableBalance = balance,
      currentBalance = balance
    )

  def buildAppeasement(payload: GiftCardCreateByCsr, originId: Int, scope: LTree): GiftCard =
    GiftCard(
      scope = scope,
      originId = originId,
      originType = GiftCard.CsrAppeasement,
      subTypeId = payload.subTypeId,
      state = GiftCard.Active,
      currency = payload.currency,
      originalBalance = payload.balance,
      availableBalance = payload.balance,
      currentBalance = payload.balance
    )

  def buildByCustomerPurchase(payload: GiftCardCreatedByCustomer, originId: Int, scope: LTree): GiftCard = {
    val message: Option[String] =
      payload.message.flatMap(msg ⇒ if (msg.trim.isEmpty) None else Option(msg.trim))
    GiftCard(
      scope = scope,
      originId = originId,
      originType = GiftCard.CustomerPurchase,
      subTypeId = payload.subTypeId,
      state = GiftCard.Active,
      currency = payload.currency,
      originalBalance = payload.balance,
      availableBalance = payload.balance,
      currentBalance = payload.balance,
      senderName = payload.senderName.some,
      recipientName = payload.recipientName.some,
      recipientEmail = payload.recipientEmail.some,
      message = message
    )
  }

  def buildScTransfer(balance: Long, originId: Int, currency: Currency, scope: LTree): GiftCard =
    GiftCard(
      scope = scope,
      originId = originId,
      originType = GiftCard.FromStoreCredit,
      state = GiftCard.Active,
      currency = currency,
      originalBalance = balance,
      availableBalance = balance,
      currentBalance = balance
    )

  def buildLineItem(balance: Long, originId: Int, currency: Currency)(implicit au: AU): GiftCard =
    GiftCard(
      scope = Scope.current,
      originId = originId,
      originType = GiftCard.CustomerPurchase,
      state = GiftCard.Cart,
      currency = currency,
      originalBalance = balance,
      availableBalance = balance,
      currentBalance = balance
    )

  def validateStateReason(state: State, reason: Option[Int]): ValidatedNel[Failure, Unit] =
    if (state == Canceled) {
      validExpr(reason.isDefined, EmptyCancellationReasonFailure.description)
    } else {
      valid({})
    }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] =
    OriginType.slickColumn
}

class GiftCards(tag: Tag) extends FoxTable[GiftCard](tag, "gift_cards") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope            = column[LTree]("scope")
  def originId         = column[Int]("origin_id")
  def originType       = column[GiftCard.OriginType]("origin_type")
  def subTypeId        = column[Option[Int]]("subtype_id")
  def code             = column[String]("code")
  def state            = column[GiftCard.State]("state")
  def currency         = column[Currency]("currency")
  def originalBalance  = column[Long]("original_balance")
  def currentBalance   = column[Long]("current_balance")
  def availableBalance = column[Long]("available_balance")
  def canceledAmount   = column[Option[Long]]("canceled_amount")
  def canceledReason   = column[Option[Int]]("canceled_reason")
  def reloadable       = column[Boolean]("reloadable")
  def accountId        = column[Option[Int]]("account_id")
  def createdAt        = column[Instant]("created_at")
  def senderName       = column[Option[String]]("sender_name")
  def recipientName    = column[Option[String]]("recipient_name")
  def recipientEmail   = column[Option[String]]("recipient_email")
  def message          = column[Option[String]]("message")

  def * =
    (id,
     scope,
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
     accountId,
     createdAt,
     senderName,
     recipientEmail,
     recipientName,
     message) <> ((GiftCard.apply _).tupled, GiftCard.unapply)
}

object GiftCards
    extends FoxTableQuery[GiftCard, GiftCards](new GiftCards(_))
    with SearchByCode[GiftCard, GiftCards] {

  import GiftCard._

  def auth(giftCard: GiftCard, orderPaymentId: Int, debit: Long = 0)(
      implicit ec: EC): DbResultT[GiftCardAdjustment] =
    adjust(giftCard, orderPaymentId.some, debit = debit, state = InStorePaymentStates.Auth)

  def authOrderPayment(giftCard: GiftCard, pmt: OrderPayment, maxPaymentAmount: Option[Long] = None)(
      implicit ec: EC): DbResultT[GiftCardAdjustment] =
    auth(giftCard = giftCard, orderPaymentId = pmt.id, debit = pmt.getAmount(maxPaymentAmount))

  def captureOrderPayment(giftCard: GiftCard, pmt: OrderPayment, maxAmount: Option[Long] = None)(
      implicit ec: EC): DbResultT[GiftCardAdjustment] =
    capture(giftCard, pmt.id, debit = pmt.getAmount(maxAmount))

  def capture(giftCard: GiftCard, orderPaymentId: Int, debit: Long)(
      implicit ec: EC): DbResultT[GiftCardAdjustment] =
    for {
      auth ← * <~ GiftCardAdjustments
              .authorizedOrderPayment(orderPaymentId)
              .mustFindOneOr(GiftCardAuthAdjustmentNotFound(orderPaymentId))
      _ ← * <~ require(debit <= auth.debit)
      cap ← * <~ GiftCardAdjustments
             .update(auth, auth.copy(debit = debit, state = InStorePaymentStates.Capture))
    } yield cap

  def cancelByCsr(giftCard: GiftCard, storeAdmin: User)(implicit ec: EC): DbResultT[GiftCardAdjustment] = {
    val adjustment = Adj(
      giftCardId = giftCard.id,
      orderPaymentId = None,
      storeAdminId = storeAdmin.accountId.some,
      debit = giftCard.availableBalance,
      credit = 0,
      availableBalance = 0,
      state = InStorePaymentStates.CancellationCapture
    )
    Adjs.create(adjustment)
  }

  def redeemToStoreCredit(giftCard: GiftCard, storeAdmin: User)(
      implicit ec: EC): DbResultT[GiftCardAdjustment] = {
    val adjustment = Adj(
      giftCardId = giftCard.id,
      orderPaymentId = None,
      storeAdminId = storeAdmin.accountId.some,
      debit = giftCard.availableBalance,
      credit = 0,
      availableBalance = 0,
      state = InStorePaymentStates.Capture
    )
    Adjs.create(adjustment)
  }

  def findByCode(code: String): QuerySeq =
    filter(_.code === code)

  def findOneByCode(code: String): DBIO[Option[GiftCard]] =
    findByCode(code).one

  def findActiveByCode(code: String): QuerySeq =
    findByCode(code).filter(_.state === (GiftCard.Active: GiftCard.State))

  def findActive(): QuerySeq =
    filter(_.state === (GiftCard.Active: GiftCard.State))

  def adjust(giftCard: GiftCard,
             orderPaymentId: Option[Int],
             debit: Long = 0,
             credit: Long = 0,
             state: InStorePaymentStates.State = InStorePaymentStates.Auth)(
      implicit ec: EC): DbResultT[GiftCardAdjustment] = {
    val balance = giftCard.availableBalance - debit + credit
    val adjustment = Adj(giftCardId = giftCard.id,
                         orderPaymentId = orderPaymentId,
                         debit = debit,
                         credit = credit,
                         availableBalance = balance,
                         state = state)
    Adjs.create(adjustment)
  }

  type Ret       = (Int, String, Long, Long)
  type PackedRet = (Rep[Int], Rep[String], Rep[Long], Rep[Long])
  val returningQuery = map { gc ⇒
    (gc.id, gc.code, gc.currentBalance, gc.availableBalance)
  }
  private val rootLens = lens[GiftCard]
  val returningLens =
    rootLens.id ~ rootLens.code ~ rootLens.currentBalance ~ rootLens.availableBalance
}
