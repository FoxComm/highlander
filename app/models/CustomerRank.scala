package models

import monocle.macros.GenLens
import utils.{ModelWithIdParameter, TableQueryWithId}
import utils.GenericTable.TableWithId
import slick.driver.PostgresDriver.api._

final case class CustomerRank(id: Int = 0, revenue: Int = 0, rank: Int = 0)
  extends ModelWithIdParameter {
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

}