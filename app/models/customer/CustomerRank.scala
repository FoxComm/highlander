package models.customer

import scala.concurrent.Future

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class CustomerRank(id: Int = 0, revenue: Int = 0, rank: Int = 0)
  extends FoxModel[CustomerRank] {
}

class CustomersRanks(tag: Tag) extends FoxTable[CustomerRank](tag, "customers_ranking") {
  def id = column[Int]("id", O.PrimaryKey)
  def revenue = column[Int]("revenue")
  def rank = column[Int]("rank")

  def * = (id, revenue, rank) <>((CustomerRank.apply _).tupled, CustomerRank.unapply)
}

object CustomersRanks extends FoxTableQuery[CustomerRank, CustomersRanks](
  idLens = lens[CustomerRank].id
)(new CustomersRanks(_)) {

  def refresh(implicit db: Database): Future[Int] = {
    db.run(sqlu"REFRESH MATERIALIZED VIEW CONCURRENTLY customers_ranking")
  }
}