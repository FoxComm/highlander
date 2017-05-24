package phoenix.models.customer

import shapeless._
import slick.jdbc.PostgresProfile.api._
import utils.db._

case class CustomerRank(id: Int = 0, revenue: Int = 0, rank: Option[Int] = Some(0))
    extends FoxModel[CustomerRank] {}

class CustomersRanks(tag: Tag) extends FoxTable[CustomerRank](tag, "customers_search_view") {
  def id      = column[Int]("id", O.PrimaryKey)
  def revenue = column[Int]("revenue")
  def rank    = column[Option[Int]]("rank")

  def * = (id, revenue, rank) <> ((CustomerRank.apply _).tupled, CustomerRank.unapply)
}

object CustomersRanks
    extends FoxTableQuery[CustomerRank, CustomersRanks](new CustomersRanks(_))
    with ReturningId[CustomerRank, CustomersRanks] {

  val returningLens: Lens[CustomerRank, Int] = lens[CustomerRank].id
}
