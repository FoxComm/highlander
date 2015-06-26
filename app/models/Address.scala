package models

import monocle.macros.GenLens
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, TableQueryWithId, Validation, RichTable}
import utils.{Validation, RichTable}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class Address(id: Int = 0, customerId: Int, stateId: Int, name: String, street1: String, street2: Option[String],
                   city: String, zip: String) extends Validation[Address] with ModelWithIdParameter {
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
            street1 = p.street1, street2 = p.street2, city = p.city, zip = p.zip)
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

  def * = (id, customerId, stateId, name, street1, street2, city, zip) <> ((Address.apply _).tupled, Address.unapply)

  def state = foreignKey("addresses_state_id_fk", stateId, TableQuery[States])(_.id)
}

object Addresses extends TableQueryWithId[Address, Addresses](
  idLens = GenLens[Address](_.id)
  )(new Addresses(_)) {

  def findAllByCustomer(customer: Customer)(implicit db: Database): Future[Seq[Address]] = {
    findAllByCustomerId(customer.id)
  }

  def findAllByCustomerId(id: Int)(implicit db: Database): Future[Seq[Address]] = {
    db.run(filter(_.customerId === id).result)
  }

  def count()(implicit ec: ExecutionContext, db: Database): Future[Int] = {
    db.run(length.result)
  }

  def createFromPayload(customer: Customer,
                        payload: Seq[CreateAddressPayload])
                       (implicit ec: ExecutionContext,
                        db: Database): Future[Seq[Address] Or Map[Address, Set[ErrorMessage]]] = {

    val addresses = payload.map(Address.fromPayload(_).copy(customerId = customer.id))

    create(customer, addresses)
  }

  def create(customer: Customer, addresses: Seq[Address])
            (implicit ec: ExecutionContext,
             db: Database): Future[Seq[Address] Or Map[Address, Set[ErrorMessage]]] = {

    val failures = addresses.map { a => (a, a.validate) }.filterNot { case (a, v) => v.isValid }

    if (failures.nonEmpty) {
      val acc = Map[Address, Set[ErrorMessage]]()
      val errorMap = failures.foldLeft(acc) { case (map, (address, failure)) =>
        map.updated(address, failure.messages)
      }
      Future.successful(Bad(errorMap))
    } else {
      db.run(for {
        _ <- this ++= addresses
        addresses <- filter(_.customerId === customer.id).result
      } yield Good(addresses))
    }
  }
}
