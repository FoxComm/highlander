package services.customerGroups

import java.time.Instant

import failures.CustomerGroupFailures.CustomerGroupMemberCannotBeDeleted
import failures.NotFoundFailure404
import models.account.{Scope, User}
import models.customer._
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import responses.DynamicGroupResponses.DynamicGroupResponse.{Root, build}
import services.LogActivity
import utils.aliases._
import utils.db._
import utils.db.ExPostgresDriver.api._
import utils.time._

object GroupManager {

  def findAll(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    CustomerGroups.filterActive().result.map(_.map(build)).dbresult

  def getById(groupId: Int)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      group ← * <~ CustomerGroups.mustFindById404(groupId)
    } yield build(group)

  def create(payload: CustomerDynamicGroupPayload,
             admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Root] =
    payload.templateId
      .map(tid ⇒ createTemplateGroup(tid, payload, admin))
      .getOrElse(createCustom(payload, admin))

  def update(groupId: Int,
             payload: CustomerDynamicGroupPayload,
             admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Root] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerGroups.mustFindById404(groupId)
      _ ← * <~ failIf(group.deletedAt.isDefined && group.deletedAt.get.isBeforeNow,
                      NotFoundFailure404(CustomerGroup, groupId))
      groupEdited ← * <~ CustomerGroups.update(
                       group,
                       CustomerDynamicGroup
                         .fromPayloadAndAdmin(payload, group.createdBy, scope)
                         .copy(id = groupId, updatedAt = Instant.now))
      _ ← * <~ LogActivity.customerGroupUpdated(groupEdited, admin)
    } yield build(groupEdited)

  def delete(groupId: Int, admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Unit] =
    for {
      scope             ← * <~ Scope.current
      group             ← * <~ CustomerGroups.mustFindById404(groupId)
      _                 ← * <~ CustomerGroups.update(group, group.copy(deletedAt = Option(Instant.now)))
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
      _ ← * <~ LogActivity.customerGroupArchived(group, admin)
    } yield DbResultT.unit

  private def createCustom(payload: CustomerDynamicGroupPayload,
                           admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Root] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerGroups.create(
                 CustomerDynamicGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
      _ ← * <~ LogActivity.customerGroupCreated(group, admin)
    } yield build(group)

  private def createTemplateGroup(
      templateId: Int,
      payload: CustomerDynamicGroupPayload,
      admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Root] =
    for {
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      template ← * <~ CustomerGroupTemplates.mustFindById404(templateId)
      group ← * <~ CustomerGroups.create(
                 CustomerDynamicGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
      _ ← * <~ GroupTemplateInstances.create(
             GroupTemplateInstance(groupId = group.id,
                                   groupTemplateId = template.id,
                                   scope = scope))
      _ ← * <~ LogActivity.customerGroupCreated(group, admin)
    } yield build(group)
}
