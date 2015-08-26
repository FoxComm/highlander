package models

import monocle.macros.GenLens
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, TableQueryWithId, Validation, RichTable}
import utils.{Validation, RichTable}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class Address(id: Int = 0, customerId: Int, stateId: Int, name: String, street1: String, street2: Option[String],
  city: String, zip: String, isDefaultShipping: Boolean = false, phoneNumber: Option[String])
  extends Validation[Address]
  with ModelWithIdParameter {

  override def validator = createValidator[Address] { address =>
    address.name is notEmpty
    address.street1 is notEmpty
    address.city is notEmpty
    address.zip should matchRegex("[0-9]{5}")
  }
}

object Address {
  def fromPayload(p: CreateAddressPayload) = {
    Address(customerId = 0, stateId = p.stateId, name = p.name,
      street1 = p.street1, street2 = p.street2, city = p.city, zip = p.zip, phoneNumber = p.phoneNumber)
  }

  def fromOrderShippingAddress(osa: OrderShippingAddress) = {
    Address(customerId = 0, stateId = osa.stateId, name = osa.name, street1 = osa.street1, street2 = osa.street2,
      city = osa.city, zip = osa.zip, phoneNumber = osa.phoneNumber)
  }
}

class Addresses(tag: Tag) extends TableWithId[Address](tag, "addresses") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def stateId = column[Int]("state_id")
  def name = column[String]("name")
  def street1 = column[String]("street1")
  def street2 = column[Option[String]]("street2")
  def city = column[String]("city")
  def zip = column[String]("zip")
  def isDefaultShipping = column[Boolean]("is_default_shipping")
  def phoneNumber = column[Option[String]]("phone_number")

  def * = (id, customerId, stateId, name, street1, street2,
    city, zip, isDefaultShipping, phoneNumber) <> ((Address.apply _).tupled, Address.unapply)

  def state = foreignKey(States.tableName, stateId, States)(_.id)
}

object Addresses extends TableQueryWithId[Address, Addresses](
  idLens = GenLens[Address](_.id)
  )(new Addresses(_)) {

  def findAllByCustomer(customer: Customer)(implicit db: Database): Future[Seq[Address]] = {
    findAllByCustomerId(customer.id)
  }

  def findAllByCustomerId(customerId: Int)(implicit db: Database): Future[Seq[Address]] =
    _findAllByCustomerId(customerId).result.run()

  def _findAllByCustomerId(customerId: Int): Query[Addresses, Address, Seq] =
    filter(_.customerId === customerId)

  def _findAllByCustomerIdWithStates(customerId: Int): Query[(Addresses, States), (Address, State), Seq] = for {
    (addresses, states) ← _withStates(_findAllByCustomerId(customerId))
  } yield (addresses, states)

  def _withStates(q: Query[Addresses, Address, Seq]) = for {
    addresses ← q
    states ← States if states.id === addresses.id
  } yield (addresses, states)

  def findShippingDefaultByCustomerId(customerId: Int): Query[Addresses, Address, Seq] =
   filter(_.customerId === customerId).filter(_.isDefaultShipping === true)
}
