package models.customer

import shapeless._
import slick.lifted.Tag
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class CustomerGroupTemplate(id: Int = 0,
                                 name: String,
                                 clientState: Json,
                                 elasticRequest: Json)
    extends FoxModel[CustomerGroupTemplate]

class CustomerGroupTemplates(tag: Tag)
    extends FoxTable[CustomerGroupTemplate](tag, "customer_group_templates") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name           = column[String]("name")
  def clientState    = column[Json]("client_state")
  def elasticRequest = column[Json]("elastic_request")

  def * =
    (id, name, clientState, elasticRequest) <>
      ((CustomerGroupTemplate.apply _).tupled, CustomerGroupTemplate.unapply)
}

object CustomerGroupTemplates
    extends FoxTableQuery[CustomerGroupTemplate, CustomerGroupTemplates](
        new CustomerGroupTemplates(_))
    with ReturningId[CustomerGroupTemplate, CustomerGroupTemplates] {

  val returningLens: Lens[CustomerGroupTemplate, Int] = lens[CustomerGroupTemplate].id

}
