package models

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.model.headers.Origin

import cats.data.ValidatedNel
import cats.implicits._
import org.joda.time.DateTime
import services.Failure
import utils.Litterbox._
import utils.Validation

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.pellucid.sealerate
import models.GiftCard.{CustomerPurchase, OnHold, OriginType, Status}
import monocle.macros.GenLens
import services.Result
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Money._
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

final case class GiftCard(id: Int = 0, originId: Int, originType: OriginType = CustomerPurchase, code: String,
  currency: Currency, status: Status = OnHold, originalBalance: Int, currentBalance: Int = 0,
  availableBalance: Int = 0, canceledReason: Option[String] = None, reloadable: Boolean = false,
  createdAt: DateTime = DateTime.now())
  extends PaymentMethod
  with ModelWithIdParameter
  with FSM[GiftCard.Status, GiftCard]
  with Validation[GiftCard] {

  import GiftCard._
  import Validation._

  def validate: ValidatedNel[Failure, GiftCard] = {
    ( notEmpty(code, "code")
      |@| notEmptyIf(canceledReason, status == Canceled, "canceledReason")
      |@| validExpr(originalBalance >= 0, "originalBalance should be greater or equal than zero")
      |@| validExpr(currentBalance >= 0, "currentBalance should be greater or equal than zero")
      ).map { case _ ⇒ this }
  }

  def stateLens = GenLens[GiftCard](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled)
  )

  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String] =
    Result.good("authenticated")

  def isActive: Boolean = activeStatuses.contains(status)

  def hasAvailable(amount: Int): Boolean = availableBalance >= amount
}

object GiftCard {
  sealed trait Status
  case object OnHold extends Status
  case object Active extends Status
  case object Canceled extends Status

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

  val activeStatuses = Set[Status](Active)

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn
}

class GiftCards(tag: Tag) extends GenericTable.TableWithId[GiftCard](tag, "gift_cards")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId = column[Int]("origin_id")
  def originType = column[GiftCard.OriginType]("origin_type")
  def code = column[String]("code")
  def status = column[GiftCard.Status]("status")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def canceledReason = column[Option[String]]("canceled_reason")
  def reloadable = column[Boolean]("reloadable")
  def createdAt = column[DateTime]("created_at")

  def * = (id, originId, originType, code, currency, status, originalBalance, currentBalance,
    availableBalance, canceledReason, reloadable, createdAt) <> ((GiftCard.apply _).tupled, GiftCard.unapply)
}

object GiftCards extends TableQueryWithId[GiftCard, GiftCards](
  idLens = GenLens[GiftCard](_.id)
  )(new GiftCards(_)){

  import GiftCard._
  import models.{GiftCardAdjustment ⇒ Adj, GiftCardAdjustments ⇒ Adjs}

  def auth(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Adj.Auth)

  def capture(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DBIO[Adj] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Adj.Capture)

  def findByCode(code: String): Query[GiftCards, GiftCard, Seq] =
    filter(_.code === code)

  def findActiveByCode(code: String): Query[GiftCards, GiftCard, Seq] =
    findByCode(code).filter(_.status inSet activeStatuses)

  override def save(giftCard: GiftCard)(implicit ec: ExecutionContext): DBIO[GiftCard] = for {
    (id, cb, ab) ← this.returning(map { gc ⇒ (gc.id, gc.currentBalance, gc.availableBalance) }) += giftCard
  } yield giftCard.copy(id = id, currentBalance = cb, availableBalance = ab)

  private def adjust(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0,
    status: Adj.Status = Adj.Auth)
    (implicit ec: ExecutionContext): DBIO[Adj] = {
    val adjustment = Adj(giftCardId = giftCard.id, orderPaymentId = orderPaymentId,
      debit = debit, credit = credit, status = status)
    Adjs.save(adjustment)
  }
}
