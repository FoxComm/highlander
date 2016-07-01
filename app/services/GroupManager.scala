package services

import models.StoreAdmin
import models.customer.{CustomerDynamicGroup, CustomerDynamicGroups}
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object GroupManager {

  // TODO move to ES
  def findAll(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    CustomerDynamicGroups.result.map(_.map(build)).toXor

  def getById(groupId: Int)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
    } yield build(group)).run()

  def create(payload: CustomerDynamicGroupPayload, admin: StoreAdmin)(implicit ec: EC,
                                                                      db: DB): Result[Root] =
    (for {
      group ← * <~ CustomerDynamicGroups.create(
                 CustomerDynamicGroup.fromPayloadAndAdmin(payload, admin.id))
    } yield build(group)).runTxn()

  def update(groupId: Int, payload: CustomerDynamicGroupPayload)(implicit ec: EC,
                                                                 db: DB): Result[Root] =
    (for {
      group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
      groupEdited ← * <~ CustomerDynamicGroups.update(
                       group,
                       CustomerDynamicGroup
                         .fromPayloadAndAdmin(payload, group.createdBy)
                         .copy(id = groupId))
    } yield build(groupEdited)).runTxn()
}
