package models.customer

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import models.account.Scope
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import shapeless._
import slick.lifted.Tag
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class CustomerDynamicGroup(id: Int = 0,
                                scope: LTree,
                                createdBy: Int,
                                name: String,
                                customersCount: Int,
                                clientState: Json,
                                elasticRequest: Json,
                                updatedAt: Instant = Instant.now,
                                createdAt: Instant = Instant.now,
                                deletedAt: Option[Instant] = None)
    extends FoxModel[CustomerDynamicGroup]

object CustomerDynamicGroup {

  def fromPayloadAndAdmin(p: CustomerDynamicGroupPayload,
                          adminId: Int,
                          scope: LTree): CustomerDynamicGroup =
    CustomerDynamicGroup(id = 0,
                         scope = scope,
                         createdBy = adminId,
                         name = p.name,
                         customersCount = p.customersCount,
                         clientState = p.clientState,
                         elasticRequest = p.elasticRequest)
}

class CustomerDynamicGroups(tag: Tag)
    extends FoxTable[CustomerDynamicGroup](tag, "customer_dynamic_groups") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope          = column[LTree]("scope")
  def createdBy      = column[Int]("created_by")
  def name           = column[String]("name")
  def customersCount = column[Int]("customers_count")
  def clientState    = column[Json]("client_state")
  def elasticRequest = column[Json]("elastic_request")
  def updatedAt      = column[Instant]("updated_at")
  def createdAt      = column[Instant]("created_at")
  def deletedAt      = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     scope,
     createdBy,
     name,
     customersCount,
     clientState,
     elasticRequest,
     updatedAt,
     createdAt,
     deletedAt) <>
      ((CustomerDynamicGroup.apply _).tupled, CustomerDynamicGroup.unapply)
}

object CustomerDynamicGroups
    extends FoxTableQuery[CustomerDynamicGroup, CustomerDynamicGroups](
        new CustomerDynamicGroups(_))
    with ReturningId[CustomerDynamicGroup, CustomerDynamicGroups] {

  val returningLens: Lens[CustomerDynamicGroup, Int] = lens[CustomerDynamicGroup].id

  def filterActive(): QuerySeq = filter(_.deletedAt.isEmpty)

}
