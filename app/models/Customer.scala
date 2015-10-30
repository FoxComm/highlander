package models

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Validation

import scala.concurrent.ExecutionContext

import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, TableQueryWithId, Validation}
import utils.Slick.implicits._
import payloads.CreateCustomerPayload
import utils.Passwords._

final case class Customer(id: Int = 0, email: String, password: Option[String] = None,
  name: Option[String] = None, isDisabled: Boolean = false, isBlacklisted: Boolean = false,
  phoneNumber: Option[String] = None, location: Option[String] = None,
  modality: Option[String] = None, isGuest: Boolean = false, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Customer]
  with Validation[Customer] {

  import Validation._

  override def validate: ValidatedNel[Failure, Customer] = {
    if (isGuest) {
      notEmpty(email, "email").map { case _ ⇒ this }
    } else {
      (notEmpty(name, "name")
        |@| notEmpty(name.getOrElse(""), "name")
        |@| matches(name.getOrElse(""), Customer.namePattern , "name")
        |@| notEmpty(email, "email")
        ).map { case _ ⇒ this }
    }
  }
}

object Customer {

  val namePattern = "[^@]+"

  def buildGuest(email: String): Customer =
    Customer(isGuest = true, email = email)

  def buildFromPayload(payload: CreateCustomerPayload): Customer = {
    val hash = payload.password.map(hashPassword(_))
    Customer(id = 0, email = payload.email, password = hash, name = payload.name,
      isGuest = payload.isGuest.getOrElse(false))
  }
}

class Customers(tag: Tag) extends TableWithId[Customer](tag, "customers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def isDisabled = column[Boolean]("is_disabled")
  def disabledBy = column[Option[Int]]("disabled_by")
  def isBlacklisted = column[Boolean]("is_blacklisted")
  def blacklistedBy = column[Option[Int]]("blacklisted_by")
  def blacklistedReason = column[Option[String]]("blacklisted_reason")
  def email = column[String]("email")
  def password = column[Option[String]]("hashed_password")
  def name = column[Option[String]]("name")
  def phoneNumber = column[Option[String]]("phone_number")
  def location = column[Option[String]]("location")
  def modality = column[Option[String]]("modality")
  def isGuest = column[Boolean]("is_guest")
  def createdAt = column[Instant]("created_at")

  def * = (id, email, password, name,
    isDisabled, isBlacklisted, phoneNumber,
    location, modality, isGuest, createdAt) <>((Customer.apply _).tupled, Customer.unapply)
}

object Customers extends TableQueryWithId[Customer, Customers](
  idLens = GenLens[Customer](_.id)
)(new Customers(_)) {

  def findByEmail(email: String): DBIO[Option[Customer]] = {
    filter(_.email === email).one
  }

  object scope {
    implicit class CustomersQuerySeqConversions(query: QuerySeq) {

      /* Returns Query with additional information like
       * included shippingRegion and billingRegion and rank for customer
       * shippingRegion comes from default address of customer
       * billingRegion comes from default creditCard of customer
       */

      def withAdditionalInfo = {
        val customerWithShipRegion = for {
          ((c, a), r) ← query.joinLeft(Addresses).on {
            case (a, b) ⇒ a.id === b.customerId && b.isDefaultShipping === true
          }.joinLeft(Regions).on(_._2.map(_.regionId) === _.id)
        } yield (c, r)

        val CcWithRegions = CreditCards.join(Regions).on {
          case (c, r) ⇒ c.regionId === r.id && c.isDefault === true && c.inWallet === true
        }

        val withRegions = for {
          ((c, shipRegion), billInfo) ←
          customerWithShipRegion.joinLeft(CcWithRegions).on(_._1.id === _._1.customerId)
        } yield (c, shipRegion, billInfo.map(_._2))

        for {
          ((c, shipRegion, billRegion), rank) ← withRegions.joinLeft(CustomersRanks).on(_._1.id === _.id)
        } yield (c, shipRegion, billRegion, rank)
      }

    }
  }
}

