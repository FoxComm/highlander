package models.customer

import java.time.Instant

import cats.data.{Validated, ValidatedNel, Xor}
import cats.implicits._
import failures.UserFailures._
import failures._
import models.location._
import models.payment.creditcard.CreditCards
import payloads.CustomerPayloads.CreateCustomerPayload
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Passwords._
import utils.Validation
import utils.aliases._
import utils.db._

case class CustomerUser(id: Int = 0,
                        userId: Int,
                        accountId: Int,
                        isGuest: Boolean = false,
                        createdAt: Instant = Instant.now,
                        udpatedAt: Instant = Instant.now,
                        deletedAt: Option[Instant] = None)
    extends FoxModel[CustomerUser]

class CustomerUsers(tag: Tag) extends FoxTable[CustomerUser](tag, "customer_users") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId    = column[Int]("user_id")
  def accountId = column[Int]("account_id")
  def isGuest   = column[Boolean]("is_guest")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * =
    (id, userId, accountId, isGuest, createdAt, updatedAt, deletedAt) <> ((CustomerUser.apply _).tupled, CustomerUser.unapply)
}

object CustomerUsers
    extends FoxTableQuery[CustomerUser, CustomerUsers](new CustomerUsers(_))
    with ReturningId[CustomerUser, CustomerUsers] {

  val returningLens: Lens[CustomerUser, Int] = lens[CustomerUser].id

  object scope {
    implicit class CustomersQuerySeqConversions(query: QuerySeq) {

      /* Returns Query with additional information like
       * included shippingRegion and billingRegion and rank for customer
       * - shippingRegion comes from default address of customer
       * - billingRegion comes from default creditCard of customer
       * - rank is calculated as percentile from net revenue
       */
      def withRegionsAndRank: Query[(CustomerUsers,
                                     Rep[Option[Regions]],
                                     Rep[Option[Regions]],
                                     Rep[Option[CustomersRanks]]),
                                    (CustomerUser,
                                     Option[Region],
                                     Option[Region],
                                     Option[CustomerRank]),
                                    Seq] = {

        val customerWithShipRegion = for {
          ((c, a), r) ← query
                         .joinLeft(Addresses)
                         .on {
                           case (a, b) ⇒
                             a.accountId === b.accountId && b.isDefaultShipping === true
                         }
                         .joinLeft(Regions)
                         .on(_._2.map(_.regionId) === _.id)
        } yield (c, r)

        val CcWithRegions = CreditCards.join(Regions).on {
          case (c, r) ⇒ c.regionId === r.id && c.isDefault === true && c.inWallet === true
        }

        val withRegions = for {
          ((c, shipRegion), billInfo) ← customerWithShipRegion
                                         .joinLeft(CcWithRegions)
                                         .on(_._1.accountId === _._1.accountId)
        } yield (c, shipRegion, billInfo.map(_._2))

        for {
          ((c, shipRegion, billRegion), rank) ← withRegions
                                                 .joinLeft(CustomersRanks)
                                                 //MAXDO Verify this is correct
                                                 .on(_._1.id === _.id)
        } yield (c, shipRegion, billRegion, rank)
      }
    }
  }

  def findGuests(email: String): DBIO[Option[CustomerUser]] = {
    filter(_.isGuest === true).one
  }

  def findOneByAccountId(accountId: Int): DBIO[Option[CustomerUser]] =
    filter(_.accountId === accountId).result.headOption

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def mustFindByAccountId(accountId: Int)(implicit ec: EC): DbResultT[CustomerUser] =
    filter(_.accountId === accountId).mustFindOneOr(UserWithAccountNotFound(accountId))

}
