package models.customer

import java.time.Instant

import shapeless._
import org.json4s.JsonAST.{JValue ⇒ Json}
import payloads.CustomerDynamicGroupPayload
import slick.lifted.Tag
import utils.http.CustomDirectives.SortAndPage
import utils.db.ExPostgresDriver.api._
import utils.db._

case class CustomerDynamicGroup(id: Int = 0,
  createdBy: Int,
  name: String,
  customersCount: Option[Int],
  clientState: Json,
  elasticRequest: Json,
  updatedAt: Instant = Instant.now,
  createdAt: Instant = Instant.now)
  extends FoxModel[CustomerDynamicGroup]

object CustomerDynamicGroup {

  def fromPayloadAndAdmin(p: CustomerDynamicGroupPayload, adminId: Int): CustomerDynamicGroup =
    CustomerDynamicGroup(id = 0,
      createdBy = adminId,
      name = p.name,
      customersCount = p.customersCount,
      clientState = p.clientState,
      elasticRequest = p.elasticRequest)
}


class CustomerDynamicGroups(tag: Tag) extends FoxTable[CustomerDynamicGroup](tag, "customer_dynamic_groups")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def createdBy = column[Int]("created_by")
  def name = column[String]("name")
  def customersCount = column[Option[Int]]("customers_count")
  def clientState = column[Json]("client_state")
  def elasticRequest = column[Json]("elastic_request")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def * = (id, createdBy, name, customersCount, clientState, elasticRequest, updatedAt, createdAt) <>
    ((CustomerDynamicGroup.apply _).tupled, CustomerDynamicGroup.unapply)
}

object CustomerDynamicGroups
  extends FoxTableQuery[CustomerDynamicGroup, CustomerDynamicGroups](new CustomerDynamicGroups(_))
  with ReturningId[CustomerDynamicGroup, CustomerDynamicGroups] {

  val returningLens: Lens[CustomerDynamicGroup, Int] = lens[CustomerDynamicGroup].id

  def sortedAndPaged(query: CustomerDynamicGroups.QuerySeq)(implicit sortAndPage: SortAndPage): CustomerDynamicGroups.QuerySeqWithMetadata  =
      query.withMetadata.sortAndPageIfNeeded { case (s, group) ⇒
      s.sortColumn match {
        case "id"                  ⇒ if (s.asc) group.id.asc                 else group.id.desc
        case "name"                ⇒ if (s.asc) group.name.asc               else group.name.desc
        case "createdAt"           ⇒ if (s.asc) group.createdAt.asc          else group.createdAt.desc
        case "updatedAt"           ⇒ if (s.asc) group.updatedAt.asc          else group.updatedAt.desc
        case "createdBy"           ⇒ if (s.asc) group.createdBy.asc          else group.createdBy.desc
        case other                 ⇒ invalidSortColumn(other)
      }
    }
}
