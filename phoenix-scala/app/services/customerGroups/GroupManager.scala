package services.customerGroups

import java.time.Instant

import failures.CustomerGroupFailures.CustomerGroupMemberCannotBeDeleted
import failures.NotFoundFailure404
import models.account.{Scope, User}
import models.customer.CustomerGroup.Manual
import models.customer._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import payloads.CustomerGroupPayloads.CustomerGroupPayload
import responses.GroupResponses.GroupResponse.{Root, build}
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

  def create(payload: CustomerGroupPayload,
             admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Root] =
    payload.templateId
      .map(tid ⇒ createTemplateGroup(tid, payload, admin))
      .getOrElse(createCustom(payload, admin))

  def update(groupId: Int,
             payload: CustomerGroupPayload,
             admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Root] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerGroups.mustFindById404(groupId)
      _ ← * <~ failIf(group.deletedAt.isDefined && group.deletedAt.get.isBeforeNow,
                      NotFoundFailure404(CustomerGroup, groupId))
      memberCount ← * <~ CustomerGroupMembers.findByGroupId(group.id).countDistinct.result
      payloadWithCount = if (group.groupType == Manual) payload.copy(customersCount = memberCount)
      else payload
      groupEdited ← * <~ CustomerGroups.update(
                       group,
                       CustomerGroup
                         .fromPayloadAndAdmin(payloadWithCount, group.createdBy, scope)
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

  private def createCustom(payload: CustomerGroupPayload,
                           admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Root] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerGroups.create(
                 CustomerGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
      updated ← * <~ doOrGood(group.elasticRequest == JObject() || group.elasticRequest == JNull,
                              CustomerGroups.update(group, withGroupQuery(group)),
                              group)
      _ ← * <~ LogActivity.customerGroupCreated(updated, admin)
    } yield build(updated)

  private def createTemplateGroup(templateId: Int, payload: CustomerGroupPayload, admin: User)(
      implicit ec: EC,
      db: DB,
      au: AU,
      ac: AC): DbResultT[Root] =
    for {
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      template ← * <~ CustomerGroupTemplates.mustFindById404(templateId)
      group ← * <~ CustomerGroups.create(
                 CustomerGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
      _ ← * <~ GroupTemplateInstances.create(
             GroupTemplateInstance(groupId = group.id,
                                   groupTemplateId = template.id,
                                   scope = scope))
      _ ← * <~ LogActivity.customerGroupCreated(group, admin)
    } yield build(group)

  private def withGroupQuery(group: CustomerGroup): CustomerGroup = {
    val groupQuery = parse(s"""{"query":
         |  {"bool":
         |    {"filter":
         |      [
         |        {"bool":
         |          {"must":
         |            [
         |              {"term":
         |                {"groups": ${group.id}}
         |              }
         |            ]
         |          }
         |        }
         |      ]
         |    }
         |  }
         |}""".stripMargin)
    group.copy(elasticRequest = groupQuery)
  }

}
