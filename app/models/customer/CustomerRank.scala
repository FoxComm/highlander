package models.customer

import scala.concurrent.Future

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, TableQueryWithId}

final case class CustomerRank(id: Int = 0, revenue: Int = 0, rank: Int = 0)
  extends ModelWithIdParameter[CustomerRank] {
}

class CustomersRanks(tag: Tag) extends TableWithId[CustomerRank](tag, "customers_ranking") {
  def id = column[Int]("id", O.PrimaryKey)
  def revenue = column[Int]("revenue")
  def rank = column[Int]("rank")

  def * = (id, revenue, rank) <>((CustomerRank.apply _).tupled, CustomerRank.unapply)
}

object CustomersRanks extends TableQueryWithId[CustomerRank, CustomersRanks](
  idLens = GenLens[CustomerRank](_.id)
)(new CustomersRanks(_)) {

  def refresh(implicit db: Database): Future[Int] = {
    db.run(sqlu"REFRESH MATERIALIZED VIEW CONCURRENTLY customers_ranking")
  }
}