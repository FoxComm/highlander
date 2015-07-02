package models

import com.pellucid.sealerate
import services.Failure
import slick.dbio
import slick.dbio.Effect.Write
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

case class GiftCard(id: Int = 0, code: String, currency: Currency, status: GiftCard.Status = GiftCard.New,
  originalBalance: Int, currentBalance: Int, canceledReason: Option[String] = None, reloadable: Boolean = false)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[GiftCard] {

  import GiftCard._

  override def validator = createValidator[GiftCard] { giftCard =>
    giftCard.status as "canceledReason" is nonEmptyIf(giftCard.status == Canceled, giftCard.canceledReason)
    giftCard.originalBalance should be >= 0
    giftCard.currentBalance should be >= 0
    giftCard.code is notEmpty
  }

  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[Failure]] = {
    Future.successful(Good("authenticated"))
  }
}

object GiftCard {
  sealed trait Status
  case object New extends Status
  case object Auth extends Status
  case object Hold extends Status
  case object Active extends Status
  case object Canceled extends Status
  case object PartiallyApplied extends Status
  case object Applied extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType = Status.slickColumn
}

class GiftCards(tag: Tag) extends GenericTable.TableWithId[GiftCard](tag, "gift_cards") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def code = column[String]("code")
  def status = column[GiftCard.Status]("status")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")
  def canceledReason = column[Option[String]]("canceled_reason")
  def reloadable = column[Boolean]("reloadable")

  def * = (id, code, currency, status, originalBalance, currentBalance,
    canceledReason, reloadable) <> ((GiftCard.apply _).tupled, GiftCard.unapply)
}

object GiftCards extends TableQueryWithId[GiftCard, GiftCards](
  idLens = GenLens[GiftCard](_.id)
  )(new GiftCards(_)){

  def adjust(giftCard: GiftCard, debit: Int = 0, credit: Int = 0, capture: Boolean)
    (implicit ec: ExecutionContext): DBIO[GiftCardAdjustment] = {
    val adjustment = GiftCardAdjustment(giftCardId = giftCard.id, debit = debit, credit = credit, capture = capture)
    GiftCardAdjustments.save(adjustment)
  }

  override def save(giftCard: GiftCard)(implicit ec: ExecutionContext): DBIO[GiftCard] = for {
    id ← returningId += giftCard.copy(currentBalance = giftCard.originalBalance)
  } yield giftCard.copy(id = id)
}
