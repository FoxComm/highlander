package models

import scala.concurrent.{ExecutionContext, Future}

import com.pellucid.sealerate
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import org.scalactic._
import services.Failures
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Money._
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, RichTable, TableQueryWithId, Validation}
import validators.nonEmptyIf
import GiftCard.{Status, OnHold}

final case class GiftCard(id: Int = 0, originId: Int, originType: String, code: String,
  currency: Currency, status: Status = OnHold, originalBalance: Int, currentBalance: Int = 0,
  availableBalance: Int = 0, canceledReason: Option[String] = None, reloadable: Boolean = false)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[GiftCard]
  with FSM[GiftCard.Status, GiftCard] {

  import GiftCard._

  override def validator = createValidator[GiftCard] { giftCard =>
    giftCard.status as "canceledReason" is nonEmptyIf(giftCard.status == Canceled, giftCard.canceledReason)
    giftCard.originalBalance should be >= 0
    giftCard.currentBalance should be >= 0
    giftCard.code is notEmpty
  }

  def stateLens = GenLens[GiftCard](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    OnHold → Set(Active, Canceled),
    Active → Set(OnHold, Canceled)
  )

  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or Failures] = {
    Future.successful(Good("authenticated"))
  }

  def isActive: Boolean = status == Active

  def hasAvailable(amount: Int): Boolean = availableBalance >= amount
}

object GiftCard {
  sealed trait Status
  case object OnHold extends Status
  case object Active extends Status
  case object Canceled extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType = Status.slickColumn
}

class GiftCards(tag: Tag) extends GenericTable.TableWithId[GiftCard](tag, "gift_cards") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def originId = column[Int]("origin_id")
  def originType = column[String]("origin_type")
  def code = column[String]("code")
  def status = column[GiftCard.Status]("status")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def availableBalance = column[Int]("available_balance")
  def canceledReason = column[Option[String]]("canceled_reason")
  def reloadable = column[Boolean]("reloadable")

  def * = (id, originId, originType, code, currency, status, originalBalance, currentBalance,
    availableBalance, canceledReason, reloadable) <> ((GiftCard.apply _).tupled, GiftCard.unapply)
}

object GiftCards extends TableQueryWithId[GiftCard, GiftCards](
  idLens = GenLens[GiftCard](_.id)
  )(new GiftCards(_)){

  import GiftCardAdjustment.{Auth, Capture, Status}

  def auth(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DBIO[GiftCardAdjustment] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Auth)

  def capture(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0)
    (implicit ec: ExecutionContext): DBIO[GiftCardAdjustment] =
    adjust(giftCard, orderPaymentId, debit = debit, credit = credit, status = Capture)

  def findByCode(code: String): Query[GiftCards, GiftCard, Seq] =
    filter(_.code === code)

  override def save(giftCard: GiftCard)(implicit ec: ExecutionContext): DBIO[GiftCard] = for {
    (id, cb, ab) ← this.returning(map { gc ⇒ (gc.id, gc.currentBalance, gc.availableBalance) }) += giftCard
  } yield giftCard.copy(id = id, currentBalance = cb, availableBalance = ab)

  private def adjust(giftCard: GiftCard, orderPaymentId: Int, debit: Int = 0, credit: Int = 0, status: Status = Auth)
    (implicit ec: ExecutionContext): DBIO[GiftCardAdjustment] = {
    val adjustment = GiftCardAdjustment(giftCardId = giftCard.id, orderPaymentId = orderPaymentId,
      debit = debit, credit = credit, status = status)
    GiftCardAdjustments.save(adjustment)
  }
}
