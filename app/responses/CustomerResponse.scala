package responses

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.{StoreAdmins, StoreAdmin}
import models.customer.{CustomerRank, Customer, CustomerWatcher, CustomerWatchers, CustomerAssignment,
CustomerAssignments}
import models.location.Region

import slick.driver.PostgresDriver.api._

object CustomerResponse {
  final case class Root(
    id: Int = 0,
    email: String,
    name: Option[String] = None,
    phoneNumber: Option[String] = None,
    location: Option[String] = None,
    modality: Option[String] = None,
    createdAt: Instant,
    disabled: Boolean,
    isGuest: Boolean,
    isBlacklisted: Boolean,
    rank: Option[Int] = None,
    totalSales: Option[Int] = None,
    numOrders: Option[Int] = None,
    billingRegion: Option[Region] = None,
    shippingRegion: Option[Region] = None,
    assignees: Seq[AssignmentResponse.Root] = Seq.empty,
    watchers: Seq[WatcherResponse.Root] = Seq.empty) extends ResponseItem

  final case class RootSimple(
    id: Int = 0,
    email: String,
    name: Option[String] = None,
    phoneNumber: Option[String] = None,
    location: Option[String] = None,
    modality: Option[String] = None,
    createdAt: Instant,
    disabled: Boolean,
    isGuest: Boolean,
    isBlacklisted: Boolean) extends ResponseItem

  def build(customer: Customer, shippingRegion: Option[Region] = None, billingRegion: Option[Region] = None,
    numOrders: Option[Int] = None, rank: Option[CustomerRank] = None, 
    assignments: Seq[(CustomerAssignment, StoreAdmin)] = Seq.empty,
    watchers: Seq[(CustomerWatcher, StoreAdmin)] = Seq.empty): Root =
  
    Root(id = customer.id,
      email = customer.email,
      name = customer.name,
      phoneNumber = customer.phoneNumber,
      location = customer.location,
      modality = customer.modality,
      createdAt = customer.createdAt,
      isGuest = customer.isGuest,
      disabled = customer.isDisabled,
      isBlacklisted = customer.isBlacklisted,
      rank = rank.map(_.rank),
      totalSales = rank.map(_.revenue),
      numOrders = numOrders,
      billingRegion = billingRegion,
      shippingRegion = shippingRegion,
      assignees = assignments.map((AssignmentResponse.buildForCustomer _).tupled),
      watchers = watchers.map((WatcherResponse.buildForCustomer _).tupled)
    )

  def buildForList(customer: Customer): RootSimple = RootSimple(
    id = customer.id,
    email = customer.email,
    name = customer.name,
    phoneNumber = customer.phoneNumber,
    location = customer.location,
    modality = customer.modality,
    createdAt = customer.createdAt,
    isGuest = customer.isGuest,
    disabled = customer.isDisabled,
    isBlacklisted = customer.isBlacklisted
  )

  def fromCustomer(customer: Customer)(implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchDetails(customer).map {
      case (assignees, watchers) ⇒
        build(
          customer = customer,
          assignments = assignees,
          watchers = watchers
        )
    }
  }

  private def fetchDetails(customer: Customer)(implicit ec: ExecutionContext, db: Database) = {
    for {
      assignments ← CustomerAssignments.filter(_.customerId === customer.id).result
      admins      ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
      watchlist   ← CustomerWatchers.filter(_.customerId === customer.id).result
      watchers    ← StoreAdmins.filter(_.id.inSetBind(watchlist.map(_.watcherId))).result
    } yield (assignments.zip(admins), watchlist.zip(watchers))
  }
}
