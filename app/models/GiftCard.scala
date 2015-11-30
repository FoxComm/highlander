package models

import java.time.Instant

import scala.concurrent.ExecutionContext

import cats.data.Validated._
import cats.data.{Xor, ValidatedNel}
import cats.implicits._
import payloads.AddGiftCardLineItem
import services._
import utils.CustomDirectives.SortAndPage
import utils.Litterbox._

import com.pellucid.sealerate
import models.GiftCard.{CustomerPurchase, OnHold, OriginType, Status}
import monocle.Lens
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType

import utils._
import utils.Money._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.Validation._
import utils.table.SearchByCode

final case class GiftCard(id: Int = 0, originId: Int, originType: OriginType = CustomerPurchase,
  code: String = "", subTypeId: Option[Int] = None, currency: Currency = Currency.USD, status: Status = GiftCard.Active,
  originalBalance: Int, currentBalance: Int = 0, availableBalance: Int = 0, canceledAmount: Option[Int] = None,
  canceledReason: Option[Int] = None, reloadable: Boolean = false, createdAt: Instant = Instant.now())
  extends PaymentMethod
  with ModelWithIdParameter[GiftCard]
  with FSM[GiftCard.Status, GiftCard]
  with Validation[GiftCard] {

  import GiftCard._
  import Validation._

  override def validate: ValidatedNel[Failure, GiftCard] = {
    val canceledWithReason: ValidatedNel[Failure, Unit] = (status, canceledAmount, canceledReason) match {
      case (Canceled, None, _) ⇒ invalidNel(GeneralFailure("canceledAmount must be present when canceled"))
      case (Canceled, _, None) ⇒ invalidNel(GeneralFailure("canceledReason must be present when canceled"))
      case _                   ⇒ valid({})
    }

    ( canceledWithReason
      // |@| notEmpty(code, "code") // FIXME: this check does not allow to create models
      |@| validExpr(originalBalance >= 0, "originalBalance should be greater or equal than zero")
      |@| validExpr(currentBalance >= 0, "currentBalance should be greater or equal than zero")
      ).map { case _ ⇒ this }
  }

  def stateLens = GenLens[GiftCard](_.status)
  override def primarySearchKeyLens: Lens[GiftCard, String] = GenLens[GiftCard](_.code)
  override def updateTo(newModel: GiftCard): Failures Xor GiftCard = super.transitionModel(newModel)

  val fsm: Map[Status, Set[Status]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled),
    Cart   → Set(Canceled)
  )

  def isActive: Boolean = status == Active
  def isCart: Boolean   = status == Cart

  def hasAvailable(amount: Int): Boolean = availableBalance >= amount

  def mustBeCart: Failures Xor GiftCard = if (isCart) Xor.Right(this) else Xor.Left(GiftCardMustBeCart(code).single)

  def mustNotBeCart: Failures Xor GiftCard = if (!isCart) Xor.Right(this) else Xor.Left(GiftCardMustNotBeCart(code).single)

  def mustBeActive: Failures Xor GiftCard = if (isActive) Xor.Right(this) else Xor.Left(GiftCardIsInactive(this).single)

  def mustHaveEnoughBalance(amount: Int): Failures Xor GiftCard =
    if (hasAvailable(amount)) Xor.Right(this) else Xor.Left(GiftCardNotEnoughBalance(this, amount).single)
}

object GiftCard {
  sealed trait Status
  case object OnHold extends Status
  case object Active extends Status
  case object Canceled extends Status
  case object Cart extends Status
  case object FullyRedeemed extends Status

  sealed trait OriginType
  case object CsrAppeasement extends OriginType
  case object CustomerPurchase extends OriginType
  case object FromStoreCredit extends OriginType
  case object RmaProcess extends OriginType

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  val giftCardCodeRegex = """([a-zA-Z0-9-_]*)""".r

  def update(giftCard: GiftCard, payload: AddGiftCardLineItem): GiftCard = {
    giftCard.copy(availableBalance = payload.balance, currentBalance = payload.balance,
      originalBalance = payload.balance, currency = payload.currency)
  }

  def buildAppeasement(payload: payloads.GiftCardCreateByCsr, originId: Int): GiftCard = {
    GiftCard(
      originId = originId,
      originType = GiftCard.CsrAppeasement,
      subTypeId = payload.subTypeId,
      status = GiftCard.Active,
      currency = payload.currency,
      originalBalance = payload.balance,
      availableBalance = payload.balance,
      currentBalance = payload.balance
    )
  }

  def buildLineItem(balance: Int, originId: Int, currency: Currency): GiftCard = {
    GiftCard(
      originId = originId,
      originType = GiftCard.CustomerPurchase,
      status = GiftCard.Cart,
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
      status = GiftCard.Cart,
      currency = currency,
      originalBalance = 0,
      availableBalance = 0,
      currentBalance = 0
    )
  }

  def validateStatusReason(status: Status, reason: Option[Int]): ValidatedNel[Failure, Unit] = {
    if (status == Canceled) {
      validExpr(reason.isDefined, "Please provide valid cancellation reason")
    } else {
      valid({})
    }
  }

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn
}

class GiftCards(tag: Tag) extends GenericTable.TableWithId[GiftCard](tag, "gift_cards")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId = column[Int]("origin_id")
  def originType = column[GiftCard.OriginType]("origin_type")
  def subTypeId = column[Option[Int]]("subtype_id")
  def code = column[String]("code")
  def status = column[GiftCard.Status]("status")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def canceledAmount = column[Option[Int]]("canceled_amount")
  def canceledReason = column[Option[Int]]("canceled_reason")
  def reloadable = column[Boolean]("reloadable")
  def createdAt = column[Instant]("created_at")

  def * = (id, originId, originType, code, subTypeId, currency, status, originalBalance, currentBalance,
    availableBalance, canceledAmount, canceledReason, reloadable, createdAt) <> ((GiftCard.apply _).tupled, GiftCard
    .unapply)
}

object GiftCards extends TableQueryWithId[GiftCard, GiftCards](
  idLens = GenLens[GiftCard](_.id)
  )(new GiftCards(_))
  with SearchByCode[GiftCard, GiftCards] {

  import GiftCard._
  import models.{GiftCardAdjustment ⇒ Adj, GiftCardAdjustments ⇒ Adjs}

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata = {
    query.withMetadata.sortAndPageIfNeeded { (s, giftCard) ⇒
      s.sortColumn match {
        case "id"               ⇒ if (s.asc) giftCard.id.asc               else giftCard.id.desc
        case "originId"         ⇒ if (s.asc) giftCard.originId.asc         else giftCard.originId.desc
        case "originType"       ⇒ if (s.asc) giftCard.originType.asc       else giftCard.originType.desc
        case "subTypeId"        ⇒ if (s.asc) giftCard.subTypeId.asc        else giftCard.subTypeId.desc
        case "code"             ⇒ if (s.asc) giftCard.code.asc             else giftCard.code.desc
        case "status"           ⇒ if (s.asc) giftCard.status.asc           else giftCard.status.desc
        case "currency"         ⇒ if (s.asc) giftCard.currency.asc         else giftCard.currency.desc
        case "originalBalance"  ⇒ if (s.asc) giftCard.originalBalance.asc  else giftCard.originalBalance.desc
        case "currentBalance"   ⇒ if (s.asc) giftCard.currentBalance.asc   else giftCard.currentBalance.desc
        case "availableBalance" ⇒ if (s.asc) giftCard.availableBalance.asc else giftCard.availableBalance.desc
        case "canceledAmount"   ⇒ if (s.asc) giftCard.canceledAmount.asc   else giftCard.canceledAmount.desc
        case "canceledReason"   ⇒ if (s.asc) giftCard.canceledReason.asc   else giftCard.canceledReason.desc
        case "reloadable"       ⇒ if (s.asc) giftCard.reloadable.asc       else giftCard.reloadable.desc
        case "createdAt"        ⇒ if (s.asc) giftCard.createdAt.asc        else giftCard.createdAt.desc
        case other              ⇒ invalidSortColumn(other)
      }
    }
  }

  def queryAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(this)

  def queryByCode(code: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    sortedAndPaged(findByCode(code))

  def auth(giftCard: GiftCard, orderPaymentId: Option[Int], debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DbResult[Adj] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Adj.Auth)

  def authOrderPayment(giftCard: GiftCard, pmt: OrderPayment)
    (implicit ec: ExecutionContext): DbResult[Adj] =
    auth(giftCard = giftCard, orderPaymentId = pmt.id.some, debit = pmt.amount.getOrElse(0))

  def capture(giftCard: GiftCard, orderPaymentId: Option[Int], debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DbResult[Adj] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Adj.Capture)

  def cancelByCsr(giftCard: GiftCard, storeAdmin: StoreAdmin)(implicit ec: ExecutionContext): DbResult[Adj] = {
    val adjustment = Adj(giftCardId = giftCard.id, orderPaymentId = None, storeAdminId = storeAdmin.id.some,
      debit = giftCard.availableBalance, credit = 0, availableBalance = 0, status = Adj.CancellationCapture)
    Adjs.create(adjustment)
  }

  def redeemToStoreCredit(giftCard: GiftCard, storeAdmin: StoreAdmin)(implicit ec: ExecutionContext): DbResult[Adj] = {
    val adjustment = Adj(giftCardId = giftCard.id, orderPaymentId = None, storeAdminId = storeAdmin.id.some,
      debit = giftCard.availableBalance, credit = 0, availableBalance = 0, status = Adj.Capture)
    Adjs.create(adjustment)
  }

  def findByCode(code: String): QuerySeq =
    filter(_.code === code)

  def findOneByCode(code: String): DBIO[Option[GiftCard]] =
    findByCode(code).one

  def findActiveByCode(code: String): QuerySeq =
    findByCode(code).filter(_.status === (GiftCard.Active: GiftCard.Status))

  type ReturningIdCodeBalances = (Int, String, Int, Int)

  val returningIdCodeAndBalance: Returning[ReturningIdCodeBalances] =
    this.returning(map { gc ⇒ (gc.id, gc.code, gc.currentBalance, gc.availableBalance) })

  def returningAction(ret: ReturningIdCodeBalances)(gc: GiftCard): GiftCard = ret match {
    case (id, code, currentBalance, availableBalance) ⇒
      gc.copy(id = id, code = code, currentBalance = currentBalance, availableBalance = availableBalance)
  }

  override def create[R](gc: GiftCard, returning: Returning[R], action: R ⇒ GiftCard ⇒ GiftCard)
    (implicit ec: ExecutionContext): DbResult[GiftCard] = super.create(gc, returningIdCodeAndBalance, returningAction)

  private def adjust(giftCard: GiftCard, orderPaymentId: Option[Int], debit: Int = 0, credit: Int = 0,
    status: Adj.Status = Adj.Auth)
    (implicit ec: ExecutionContext): DbResult[Adj] = {
    val balance = giftCard.availableBalance - debit + credit
    val adjustment = Adj(giftCardId = giftCard.id, orderPaymentId = orderPaymentId,
      debit = debit, credit = credit, availableBalance = balance, status = status)
    Adjs.create(adjustment)
  }

}
