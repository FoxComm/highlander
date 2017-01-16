package services.customerGroups

import models.account.{Scope, User}
import models.customer.{CustomerDynamicGroup, CustomerDynamicGroups}
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponse.{Root, build}
import utils.aliases._
import utils.db._

object GroupManager {

  def getById(groupId: Int)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
    } yield build(group)

  def create(payload: CustomerDynamicGroupPayload,
             admin: User)(implicit ec: EC, db: DB, au: AU): DbResultT[Root] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerDynamicGroups.create(
                 CustomerDynamicGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
    } yield build(group)

  def update(groupId: Int, payload: CustomerDynamicGroupPayload)(implicit ec: EC,
                                                                 db: DB,
                                                                 au: AU): DbResultT[Root] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
      groupEdited ← * <~ CustomerDynamicGroups.update(
                       group,
                       CustomerDynamicGroup
                         .fromPayloadAndAdmin(payload, group.createdBy, scope)
                         .copy(id = groupId))
    } yield build(groupEdited)
}
