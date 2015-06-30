package models

import com.pellucid.sealerate
import services.{Failure, OrderTotaler}
import utils.Money._
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class GiftCard(id: Int = 0, currency: Currency, status: GiftCard.Status, originalBalance: Int, currentBalance: Int)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[GiftCard] {

  override def validator = createValidator[GiftCard] { gc => }

  // TODO: not sure we use this polymorphically
  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[Failure]] = {
    Future.successful(Good("authenticated"))
  }
}

object GiftCard {
  sealed trait Status
  case object Hold extends Status
  case object Active extends Status
  case object Canceled extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType = Status.slickColumn
}

class GiftCards(tag: Tag) extends GenericTable.TableWithId[GiftCard](tag, "gift_cards") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def status = column[GiftCard.Status]("status")
  def currency = column[Currency]("currency")
  def originalBalance = column[Int]("original_balance")
  def currentBalance = column[Int]("current_balance")

  def * = (id, currency, status, originalBalance, currentBalance) <> ((GiftCard.apply _).tupled, GiftCard.unapply)
}

object GiftCards extends TableQueryWithId[GiftCard, GiftCards](
  idLens = GenLens[GiftCard](_.id)
  )(new GiftCards(_)){
}
