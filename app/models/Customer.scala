package models

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Validation

import scala.concurrent.{ExecutionContext, Future}

import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import services.Result
import slick.driver.PostgresDriver.api._
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, TableQueryWithId, Validation}
import utils.Slick.implicits._

final case class Customer(id: Int = 0, email: String, password: String, firstName: String, lastName: String,
  isDisabled: Boolean = false, isBlacklisted: Boolean = false,
  phoneNumber: Option[String] = None, location: Option[String] = None,
  modality: Option[String] = None, isGuest: Boolean = false, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter
  with Validation[Customer] {

  import Validation._

  def validate: ValidatedNel[Failure, Customer] = {
    if (isGuest) {
      notEmpty(email, "email").map { case _ ⇒ this }
    } else {
      (notEmpty(firstName, "firstName")
        |@| notEmpty(lastName, "lastName")
        |@| notEmpty(email, "email")
        ).map { case _ ⇒ this }
    }
  }
}

object Customer {
  def buildGuest(email: String): Customer =
    Customer(isGuest = true, email = email, firstName = "guest", lastName = "guest", password = "guest")
}

class Customers(tag: Tag) extends TableWithId[Customer](tag, "customers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def isDisabled = column[Boolean]("is_disabled")
  def disabledBy = column[Option[Int]]("disabled_by")
  def isBlacklisted = column[Boolean]("is_blacklisted")
  def blacklistedBy = column[Option[Int]]("blacklisted_by")
  def blacklistedReason = column[Option[String]]("blacklisted_reason")
  def email = column[String]("email")
  def password = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def phoneNumber = column[Option[String]]("phone_number")
  def location = column[Option[String]]("location")
  def modality = column[Option[String]]("modality")
  def isGuest = column[Boolean]("is_guest")
  def createdAt = column[Instant]("created_at")

  def * = (id, email, password, firstName, lastName,
    isDisabled, isBlacklisted, phoneNumber,
    location, modality, isGuest, createdAt) <>((Customer.apply _).tupled, Customer.unapply)
}

object Customers extends TableQueryWithId[Customer, Customers](
  idLens = GenLens[Customer](_.id)
)(new Customers(_)) {

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): DBIO[Option[Customer]] = {
    filter(_.email === email).one
  }

  def findById(id: Int)(implicit db: Database): Future[Option[Customer]] = {
    db.run(_findById(id).extract.one)
  }

  def _findById(id: Rep[Int]) = {
    filter(_.id === id)
  }

  def createFromPayload(payload: payloads.CreateCustomer)
    (implicit ec: ExecutionContext, db: Database): Result[Customer] = {
    val newCustomer = Customer(id = 0, email = payload.email, password = payload.password,
      firstName = payload.firstName, lastName = payload.firstName)

    save(newCustomer).run().flatMap(Result.right)
  }

  object scope {
    implicit class CustomersQuerySeqConversions(query: QuerySeq) {
      /* Returns Query with included shippingRegion and billingRegion for customer.
       * shippingRegion comes from default address of customer
       * billingRegion comes from default creditCard of customer
       */
      def withDefaultRegions = {
        val customerWithShipRegion = for {
          ((c, a), r) ← query.joinLeft(Addresses).on {
            case (a, b) ⇒ a.id === b.customerId && b.isDefaultShipping === true
          }.joinLeft(Regions).on(_._2.map(_.regionId) === _.id)
        } yield (c, r)

        val CcWithRegions = CreditCards.join(Regions).on {
          case (c, r) ⇒ c.regionId === r.id && c.isDefault === true
        }

        for {
          ((c, shipRegion), billInfo) ←
          customerWithShipRegion.joinLeft(CcWithRegions).on(_._1.id === _._1.customerId)
        } yield (c, shipRegion, billInfo.map(_._2))
      }
    }
  }
}

