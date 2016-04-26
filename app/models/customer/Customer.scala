package models.customer

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import failures.CustomerFailures.CustomerEmailNotUnique
import failures.Failure
import models.location._
import models.payment.creditcard.CreditCards
import shapeless._
import payloads.CreateCustomerPayload
import slick.driver.PostgresDriver.api._
import utils.Passwords._
import utils.Validation
import utils.aliases.EC
import utils.db._

case class Customer(id: Int = 0, email: String, hashedPassword: Option[String] = None,
  name: Option[String] = None, isDisabled: Boolean = false, disabledBy: Option[Int] = None,
  isBlacklisted: Boolean = false, blacklistedBy: Option[Int] = None,
  phoneNumber: Option[String] = None, location: Option[String] = None,
  modality: Option[String] = None, isGuest: Boolean = false, createdAt: Instant = Instant.now)
  extends FoxModel[Customer]
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
    build(email = payload.email, password = payload.password, name = payload.name,
      isGuest = payload.isGuest.getOrElse(false))
  }

  def build(id: Int = 0, email: String, name: Option[String] = None, isGuest: Boolean = false,
    password: Option[String] = None, phoneNumber: Option[String] = None, modality: Option[String] = None,
    location: Option[String] = None, isDisabled: Boolean = false): Customer = {

    val optHash = password.map(hashPassword)
    Customer(id = id, name = name, email = email, hashedPassword = optHash, isGuest = isGuest,
      phoneNumber = phoneNumber, modality = modality, location = location, isDisabled = isDisabled)
  }
}

class Customers(tag: Tag) extends FoxTable[Customer](tag, "customers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def isDisabled = column[Boolean]("is_disabled")
  def disabledBy = column[Option[Int]]("disabled_by")
  def isBlacklisted = column[Boolean]("is_blacklisted")
  def blacklistedBy = column[Option[Int]]("blacklisted_by")
  def blacklistedReason = column[Option[String]]("blacklisted_reason")
  def email = column[String]("email")
  def hashedPassword = column[Option[String]]("hashed_password")
  def name = column[Option[String]]("name")
  def phoneNumber = column[Option[String]]("phone_number")
  def location = column[Option[String]]("location")
  def modality = column[Option[String]]("modality")
  def isGuest = column[Boolean]("is_guest")
  def createdAt = column[Instant]("created_at")

  def * = (id, email, hashedPassword, name,
    isDisabled, disabledBy, isBlacklisted, blacklistedBy, phoneNumber,
    location, modality, isGuest, createdAt) <>((Customer.apply _).tupled, Customer.unapply)
}

object Customers extends FoxTableQuery[Customer, Customers](
  idLens = lens[Customer].id
)(new Customers(_)) {

  def findByEmail(email: String): DBIO[Option[Customer]] = {
    filter(_.email === email).one
  }

  object scope {
    implicit class CustomersQuerySeqConversions(query: QuerySeq) {

      /* Returns Query with additional information like
       * included shippingRegion and billingRegion and rank for customer
       * - shippingRegion comes from default address of customer
       * - billingRegion comes from default creditCard of customer
       * - rank is calculated as percentile from net revenue
       */
      def withRegionsAndRank: Query[(Customers, Rep[Option[Regions]], Rep[Option[Regions]],
        Rep[Option[CustomersRanks]]), (Customer, Option[Region], Option[Region], Option[CustomerRank]), Seq] = {

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

  def activeCustomerByEmail(email: String): QuerySeq =
    filter(c ⇒ c.email === email && !c.isBlacklisted && !c.isDisabled && !c.isGuest)

  def createEmailMustBeUnique(email: String)(implicit ec: EC): DbResult[Unit] =
    activeCustomerByEmail(email).one.mustNotFindOr(CustomerEmailNotUnique)

  def updateEmailMustBeUnique(email: String, customerId: Int)(implicit ec: EC): DbResult[Unit] =
    activeCustomerByEmail(email).filterNot(_.id === customerId).one.mustNotFindOr(CustomerEmailNotUnique)

}

