package responses

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import models._
import slick.driver.PostgresDriver.api._
import services._

import utils.Slick._
import utils.Slick.implicits._

object AllCustomers {
  type Response = Future[Seq[Root]]

  final case class Root(
    id: Int,
    email: String,
    name: String,
    joinedAt: Instant,
    blacklisted: Boolean,
    rank: String,
    billRegion: Option[String],
    shipRegion: Option[String]
    )


  def findAll(implicit db: Database, ec: ExecutionContext): Response = {
    val customersWithShipRegion = Customers.joinLeft(models.Addresses).on { case(a,b) => a.id === b.customerId  && b
      .isDefaultShipping === true}.joinLeft(Regions).on(_._2.map(_.regionId) === _.id)


    val creditCardsWithRegion = for {
      (c, r) ← CreditCards.join(Regions).on(_.regionId === _.id)
    } yield (c, r)

    val query = customersWithShipRegion.joinLeft(creditCardsWithRegion).on(_._1._1.id === _._1.customerId)

    db.run(query.result).map { results ⇒
      results.map {
         case (((customer, _), shipRegion), billRegion) ⇒
          build(customer, shipRegion, billRegion.map(_._2))
      }
    }


  }

  def build(customer: Customer, shipRegion: Option[Region] = None, billRegion: Option[Region] = None)
    (implicit ec: ExecutionContext): Root = {
    Root(id = customer.id,
      email =  customer.email,
      name = customer.firstName + " " + customer.lastName,
      joinedAt = customer.createdAt,
      blacklisted = customer.disabled,
      rank = "top 10",
      billRegion = billRegion.map(_.name),
      shipRegion = shipRegion.map(_.name))
  }

}
