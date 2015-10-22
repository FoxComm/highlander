package models

import java.time.Instant

import scala.concurrent.ExecutionContext

import cats.data.Validated._
import cats.data.ValidatedNel
import cats.implicits._
import services._
import utils.Litterbox._

import com.pellucid.sealerate
import models.GiftCard.{CustomerPurchase, OnHold, OriginType, Status}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType

import utils._
import utils.Money._
import utils.Validation._

final case class GiftCard(id: Int = 0, originId: Int, originType: OriginType = CustomerPurchase,
  code: String = "", subTypeId: Option[Int] = None, currency: Currency = Currency.USD, status: Status = GiftCard.Active,
  originalBalance: Int, currentBalance: Int = 0, availableBalance: Int = 0, canceledAmount: Option[Int] = None,
  canceledReason: Option[Int] = None, reloadable: Boolean = false, createdAt: Instant = Instant.now())
  extends PaymentMethod
  with ModelWithIdParameter
  with FSM[GiftCard.Status, GiftCard]
  with Validation[GiftCard] {

  import GiftCard._
  import Validation._

  def isNew: Boolean = id == 0

  def validate: ValidatedNel[Failure, GiftCard] = {
    val canceledWithReason: ValidatedNel[Failure, Unit] = (status, canceledAmount, canceledReason) match {
      case (Canceled, None, _) ⇒ invalidNel(GeneralFailure("canceledAmount must be present when canceled"))
      case (Canceled, _, None) ⇒ invalidNel(GeneralFailure("canceledReason must be present when canceled"))
      case _                   ⇒ valid({})
    }

    ( canceledWithReason
      |@| notEmpty(code, "code")
      |@| validExpr(originalBalance >= 0, "originalBalance should be greater or equal than zero")
      |@| validExpr(currentBalance >= 0, "currentBalance should be greater or equal than zero")
      ).map { case _ ⇒ this }
  }

  def stateLens = GenLens[GiftCard](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled),
    Cart   → Set(Canceled)
  )

  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String] =
    Result.good("authenticated")

  def isActive: Boolean = status == Active
  def isCart: Boolean   = status == Cart

  def hasAvailable(amount: Int): Boolean = availableBalance >= amount
}

object GiftCard {
  sealed trait Status
  case object OnHold extends Status
  case object Active extends Status
  case object Canceled extends Status
  case object Cart extends Status
  case object FullyRedeemed extends Status

  sealed trait OriginType
  case object CustomerPurchase extends OriginType
  case object CsrAppeasement extends OriginType
  case object FromStoreCredit extends OriginType

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  val giftCardCodeRegex = """([a-zA-Z0-9-_]*)""".r

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
  )(new GiftCards(_)){

  import GiftCard._
  import models.{GiftCardAdjustment ⇒ Adj, GiftCardAdjustments ⇒ Adjs}

  override def primarySearchTerm: String = "code"

  def auth(giftCard: GiftCard, orderPaymentId: Option[Int], debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Adj.Auth)

  def capture(giftCard: GiftCard, orderPaymentId: Option[Int], debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Adj.Capture)

  def cancelByCsr(giftCard: GiftCard, storeAdmin: StoreAdmin)(implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(giftCardId = giftCard.id, orderPaymentId = None, storeAdminId = storeAdmin.id.some,
      debit = giftCard.availableBalance, credit = 0, availableBalance = 0, status = Adj.CancellationCapture)
    Adjs.save(adjustment)
  }

  def redeemToStoreCredit(giftCard: GiftCard, storeAdmin: StoreAdmin)(implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(giftCardId = giftCard.id, orderPaymentId = None, storeAdminId = storeAdmin.id.some,
      debit = giftCard.availableBalance, credit = 0, availableBalance = 0, status = Adj.Capture)
    Adjs.save(adjustment)
  }

  def findByCode(code: String): Query[GiftCards, GiftCard, Seq] =
    filter(_.code === code)

  def findActiveByCode(code: String): Query[GiftCards, GiftCard, Seq] =
    findByCode(code).filter(_.status === (GiftCard.Active: GiftCard.Status))

  def findCartByCode(code: String) : Query[GiftCards, GiftCard, Seq] =
    findByCode(code).filter(_.status === (GiftCard.Cart: GiftCard.Status))

  val returningIdCodeAndBalance = this.returning(map { gc ⇒ (gc.id, gc.code, gc.currentBalance, gc.availableBalance) })

  def create(gc: GiftCard)(implicit ec: ExecutionContext): DBIO[models.GiftCard] = for {
    (newId, code, currentBalance, availableBalance) <- returningIdCodeAndBalance += gc
  } yield gc.copy(id = newId, code = code, currentBalance = currentBalance, availableBalance = availableBalance)

  override def save(gc: GiftCard)(implicit ec: ExecutionContext) = {
    if (gc.isNew) {
      create(gc)
    } else {
      super.save(gc)
    }
  }

  private def adjust(giftCard: GiftCard, orderPaymentId: Option[Int], debit: Int = 0, credit: Int = 0,
    status: Adj.Status = Adj.Auth)
    (implicit ec: ExecutionContext): DBIO[Adj] = {
    val balance = giftCard.availableBalance - debit + credit
    val adjustment = Adj(giftCardId = giftCard.id, orderPaymentId = orderPaymentId,
      debit = debit, credit = credit, availableBalance = balance, status = status)
    Adjs.save(adjustment)
  }
}
