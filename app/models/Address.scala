package models

import scala.concurrent.{ExecutionContext, Future}

import cats.data.ValidatedNel
import cats.data.Validated.{invalid, valid, invalidNel}
import cats.implicits._
import utils.Litterbox._
import monocle.macros.GenLens
import payloads.CreateAddressPayload
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.Validation.{matches ⇒ matchesNew, notEmpty ⇒ notEmptyNew}
import utils.{Model, ModelWithIdParameter, NewModel, RichTable, TableQueryWithId, Validation}

final case class Address(id: Int = 0, customerId: Int, regionId: Int, name: String,
  street1: String, street2: Option[String], city: String, zip: String,
  isDefaultShipping: Boolean = false, phoneNumber: Option[String])
  extends ModelWithIdParameter
  with NewModel {

  def isNew: Boolean = id == 0

  def sanitize: Address = {
    if (Country.usRegions.contains(regionId)) {
      this.copy(zip = zip.replace("-", ""))
    } else {
      this
    }
  }

  def validateNew: ValidatedNel[String, Model] = {
    val phone: ValidatedNel[String, Unit] = (Country.usRegions.contains(regionId), phoneNumber) match {
      case (true, Some(number)) ⇒
        matchesNew(number, "[0-9]{10}", "phoneNumber")
      case (false, Some(number)) ⇒
        matchesNew(number, "[0-9]{0,15}", "phoneNumber")
      case (_, None) ⇒
        valid({})
    }

    val zipValidation: ValidatedNel[String, Unit] = (Country.usRegions.contains(regionId), zip) match {
      case (true, zipValue) ⇒
        matchesNew(zipValue, Address.zipPatternUs, "zip")
      case (false, zipValue) ⇒
        matchesNew(zipValue, Address.zipPattern, "zip")
    }

    ( notEmptyNew(name, "name")
      |@| notEmptyNew(street1, "street1")
      |@| notEmptyNew(city, "city")
      |@| zipValidation
      |@| phone
    ).map { case _ ⇒ this }
  }
}

object Address {
  val zipPattern = "(?i)^[a-z0-9][a-z0-9\\- ]{0,10}[a-z0-9]$"
  val zipPatternUs = "^\\d{5}(?:\\d{4})?$"

  def fromPayload(p: CreateAddressPayload) = {
    Address(customerId = 0, regionId = p.regionId, name = p.name,
      street1 = p.street1, street2 = p.street2, city = p.city, zip = p.zip, phoneNumber = p.phoneNumber)
  }

  def fromOrderShippingAddress(osa: OrderShippingAddress) = {
    Address(customerId = 0, regionId = osa.regionId, name = osa.name, street1 = osa.street1, street2 = osa.street2,
      city = osa.city, zip = osa.zip, phoneNumber = osa.phoneNumber)
  }
}

class Addresses(tag: Tag) extends TableWithId[Address](tag, "addresses") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def regionId = column[Int]("region_id")
  def name = column[String]("name")
  def street1 = column[String]("street1")
  def street2 = column[Option[String]]("street2")
  def city = column[String]("city")
  def zip = column[String]("zip")
  def isDefaultShipping = column[Boolean]("is_default_shipping")
  def phoneNumber = column[Option[String]]("phone_number")

  def * = (id, customerId, regionId, name, street1, street2,
    city, zip, isDefaultShipping, phoneNumber) <> ((Address.apply _).tupled, Address.unapply)

  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
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

  def _findAllByCustomerIdWithRegions(customerId: Int): Query[(Addresses, Regions), (Address, Region), Seq] = for {
    (addresses, regions) ← _withRegions(_findAllByCustomerId(customerId))
  } yield (addresses, regions)

  def _withRegions(q: Query[Addresses, Address, Seq]) = for {
    addresses ← q
    regions ← Regions if regions.id === addresses.regionId
  } yield (addresses, regions)

  def findShippingDefaultByCustomerId(customerId: Int): Query[Addresses, Address, Seq] =
   filter(_.customerId === customerId).filter(_.isDefaultShipping === true)
}
