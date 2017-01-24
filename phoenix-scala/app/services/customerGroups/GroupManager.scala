package services.customerGroups

import java.time.Instant

import failures.CustomerGroupFailures.CustomerGroupMemberCannotBeDeleted
import failures.NotFoundFailure404
import models.account.{Scope, User}
import models.customer._
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponse.{Root, build}
import utils.aliases._
import utils.db._
import utils.db.ExPostgresDriver.api._
import utils.time._

object GroupManager {

  def getById(groupId: Int)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
    } yield build(group)

  def create(payload: CustomerDynamicGroupPayload,
             admin: User)(implicit ec: EC, db: DB, au: AU): DbResultT[Root] =
    payload.templateId
      .map(tid ⇒ createTemplateGroup(tid, payload, admin))
      .getOrElse(createCustom(payload, admin))

  def update(groupId: Int, payload: CustomerDynamicGroupPayload)(implicit ec: EC,
                                                                 db: DB,
                                                                 au: AU): DbResultT[Root] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
      _ ← * <~ failIf(group.deletedAt.isDefined && group.deletedAt.get.isBeforeNow,
                      NotFoundFailure404(CustomerDynamicGroup, groupId))
      groupEdited ← * <~ CustomerDynamicGroups.update(
                       group,
                       CustomerDynamicGroup
                         .fromPayloadAndAdmin(payload, group.createdBy, scope)
                         .copy(id = groupId, updatedAt = Instant.now))
    } yield build(groupEdited)

  def delete(groupId: Int)(implicit ec: EC, db: DB, au: AU): DbResultT[Unit] =
    for {
      scope             ← * <~ Scope.current
      group             ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
      _                 ← * <~ CustomerDynamicGroups.update(group, group.copy(deletedAt = Option(Instant.now)))
      templateInstances ← * <~ GroupTemplateInstances.findByScopeAndGroupId(scope, group.id).result
      _ ← * <~ templateInstances.map { template ⇒
           GroupTemplateInstances.update(template, template.copy(deletedAt = Option(Instant.now)))
         }
      members ← * <~ CustomerGroupMembers.findByGroupId(groupId).result
      _ ← * <~ members.map { member ⇒
            CustomerGroupMembers.deleteById(
                member.id,
                DbResultT.unit,
                id ⇒ CustomerGroupMemberCannotBeDeleted(groupId, member.id))
          }
    } yield DbResultT.unit

  private def createCustom(payload: CustomerDynamicGroupPayload,
                           admin: User)(implicit ec: EC, db: DB, au: AU): DbResultT[Root] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerDynamicGroups.create(
                 CustomerDynamicGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
    } yield build(group)

  private def createTemplateGroup(templateId: Int,
                                  payload: CustomerDynamicGroupPayload,
                                  admin: User)(implicit ec: EC, db: DB, au: AU): DbResultT[Root] =
    for {
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      template ← * <~ CustomerGroupTemplates.mustFindById404(templateId)
      group ← * <~ CustomerDynamicGroups.create(
                 CustomerDynamicGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
      _ ← * <~ GroupTemplateInstances.create(
             GroupTemplateInstance(groupId = group.id,
                                   groupTemplateId = template.id,
                                   scope = scope))
    } yield build(group)
}
