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

case class StoreCredit(id: Int = 0, currency: Currency, status: StoreCredit.Status)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[StoreCredit] {

  override def validator = createValidator[StoreCredit] { gc => }

  // TODO: not sure we use this polymorphically
  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[Failure]] = {
    Future.successful(Good("authenticated"))
  }
}

object StoreCredit {
  sealed trait Status
  case object Hold extends Status
  case object Active extends Status
  case object Canceled extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType = Status.slickColumn
}

class StoreCredits(tag: Tag) extends GenericTable.TableWithId[StoreCredit](tag, "store_credits") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def currency = column[Currency]("currency")
  def status = column[StoreCredit.Status]("status")
  def * = (id, currency, status) <> ((StoreCredit.apply _).tupled, StoreCredit.unapply)
}

object StoreCredits extends TableQueryWithId[StoreCredit, StoreCredits](
  idLens = GenLens[StoreCredit](_.id)
  )(new StoreCredits(_)){
}
