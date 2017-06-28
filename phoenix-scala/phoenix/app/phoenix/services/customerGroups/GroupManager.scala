package phoenix.services.customerGroups

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.NotFoundFailure404
import org.json4s._
import org.json4s.jackson.JsonMethods._
import phoenix.models.account.{Scope, User}
import phoenix.models.customer.CustomerGroup.Manual
import phoenix.models.customer._
import phoenix.payloads.CustomerGroupPayloads.CustomerGroupPayload
import phoenix.responses.GroupResponses.GroupResponse
import phoenix.services.LogActivity
import phoenix.utils.aliases._
import phoenix.utils.time._

object GroupManager {

  def findAll(implicit ec: EC, db: DB): DbResultT[Seq[GroupResponse]] =
    CustomerGroups.filterActive().result.map(_.map(GroupResponse.build)).dbresult

  def getById(groupId: Int)(implicit ec: EC, db: DB): DbResultT[GroupResponse] =
    for {
      group ← * <~ CustomerGroups.mustFindById404(groupId)
    } yield GroupResponse.build(group)

  def create(payload: CustomerGroupPayload,
             admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[GroupResponse] =
    payload.templateId
      .map(tid ⇒ createTemplateGroup(tid, payload, admin))
      .getOrElse(createCustom(payload, admin))

  def update(groupId: Int,
             payload: CustomerGroupPayload,
             admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[GroupResponse] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerGroups.mustFindById404(groupId)
      _ ← * <~ failIf(group.deletedAt.isDefined && group.deletedAt.get.isBeforeNow,
                      NotFoundFailure404(CustomerGroup, groupId))
      memberCount ← * <~ CustomerGroupMembers.findByGroupId(group.id).distinct.length.result
      payloadWithCount = if (group.groupType == Manual) payload.copy(customersCount = memberCount)
      else payload
      groupEdited ← * <~ CustomerGroups.update(
                     group,
                     CustomerGroup
                       .fromPayloadAndAdmin(payloadWithCount, group.createdBy, scope)
                       .copy(id = groupId, createdAt = group.createdAt, updatedAt = Instant.now))
      _ ← * <~ LogActivity().customerGroupUpdated(groupEdited, admin)
    } yield GroupResponse.build(groupEdited)

  def delete(groupId: Int, admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[Unit] =
    for {
      scope             ← * <~ Scope.current
      group             ← * <~ CustomerGroups.mustFindById404(groupId)
      _                 ← * <~ CustomerGroups.update(group, group.copy(deletedAt = Option(Instant.now)))
      templateInstances ← * <~ GroupTemplateInstances.findByScopeAndGroupId(scope, group.id).result
      _ ← * <~ templateInstances.map { template ⇒
           GroupTemplateInstances.update(template, template.copy(deletedAt = Option(Instant.now)))
         }
      _ ← * <~ CustomerGroupMembers.findByGroupId(groupId).delete
      _ ← * <~ LogActivity().customerGroupArchived(group, admin)
    } yield DbResultT.unit

  private def createCustom(payload: CustomerGroupPayload,
                           admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[GroupResponse] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      group ← * <~ CustomerGroups.create(CustomerGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
      updated ← * <~ doOrGood(group.elasticRequest == JObject() || group.elasticRequest == JNull,
                              CustomerGroups.update(group, withGroupQuery(group)),
                              group)
      _ ← * <~ LogActivity().customerGroupCreated(updated, admin)
    } yield GroupResponse.build(updated)

  private def createTemplateGroup(
      templateId: Int,
      payload: CustomerGroupPayload,
      admin: User)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[GroupResponse] =
    for {
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      template ← * <~ CustomerGroupTemplates.mustFindById404(templateId)
      group    ← * <~ CustomerGroups.create(CustomerGroup.fromPayloadAndAdmin(payload, admin.accountId, scope))
      _ ← * <~ GroupTemplateInstances.create(
           GroupTemplateInstance(groupId = group.id, groupTemplateId = template.id, scope = scope))
      _ ← * <~ LogActivity().customerGroupCreated(group, admin)
    } yield GroupResponse.build(group)

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
