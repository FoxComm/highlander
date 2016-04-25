package services

import models.customer.{CustomerDynamicGroups, CustomerDynamicGroup}
import models.StoreAdmin
import payloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponse.{Root, build}
import responses.TheResponse
import utils.http.CustomDirectives.SortAndPage
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object GroupManager {

  def findAll(implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] = {
    CustomerDynamicGroups.sortedAndPaged(CustomerDynamicGroups.query).result.map(groups ⇒
      groups.map(build)
    ).toTheResponse.run()
  }

  def getById(groupId: Int)(implicit ec: EC, db: DB): Result[Root] = (for {
    group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
  } yield build(group)).run()

  def create(payload: CustomerDynamicGroupPayload, admin: StoreAdmin)(implicit ec: EC, db: DB): Result[Root] = (for {
      group ← * <~ CustomerDynamicGroups.create(CustomerDynamicGroup.fromPayloadAndAdmin(payload, admin.id))
    } yield build(group)).runTxn()

  def update(groupId: Int, payload: CustomerDynamicGroupPayload)(implicit ec: EC, db: DB): Result[Root] = (for {
    group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
    groupEdited ← * <~ CustomerDynamicGroups.update(group,
      CustomerDynamicGroup.fromPayloadAndAdmin(payload, group.createdBy).copy(id = groupId))
  } yield build(groupEdited)).runTxn()
}
