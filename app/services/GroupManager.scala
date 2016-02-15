package services

import models.customer.{CustomerDynamicGroups, CustomerDynamicGroup}
import models.StoreAdmin
import payloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponse.{Root, build}
import responses.TheResponse
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object GroupManager {

  def findAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] = {
    CustomerDynamicGroups.sortedAndPaged(CustomerDynamicGroups.query).result.map(groups ⇒
      groups.map(build)
    ).toTheResponse.run()
  }

  def getById(groupId: Int)(implicit  db: Database, ec: ExecutionContext): Result[Root] = (for {
    group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
  } yield build(group)).run()

  def create(payload: CustomerDynamicGroupPayload, admin: StoreAdmin)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = (for {
      group ← * <~ CustomerDynamicGroups.create(CustomerDynamicGroup.fromPayloadAndAdmin(payload, admin.id))
    } yield build(group)).runTxn()

  def update(groupId: Int, payload: CustomerDynamicGroupPayload)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = (for {
    group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
    groupEdited ← * <~ CustomerDynamicGroups.update(group,
      CustomerDynamicGroup.fromPayloadAndAdmin(payload, group.createdBy).copy(id = groupId))
  } yield build(groupEdited)).runTxn()
}
