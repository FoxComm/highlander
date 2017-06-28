package phoenix.services.customerGroups

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.{NotFoundFailure400, NotFoundFailure404}
import org.json4s.JsonAST._
import phoenix.failures.CustomerGroupFailures.CustomerGroupMemberPayloadContainsSameIdsInBothSections
import phoenix.models.account.{User, Users}
import phoenix.models.cord.Orders
import phoenix.models.customer.CustomerGroup._
import phoenix.models.customer.CustomersData.scope._
import phoenix.models.customer._
import phoenix.models.discount.SearchReference
import phoenix.payloads.CustomerGroupPayloads._
import phoenix.responses.GroupResponses.CustomerGroupResponse
import phoenix.responses.users.CustomerResponse
import phoenix.services.StoreCreditService
import phoenix.services.customers.CustomerManager
import phoenix.utils.ElasticsearchApi
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

object GroupMemberManager {

  def sync(groupId: Int, payload: CustomerGroupMemberServiceSyncPayload)(implicit ec: EC,
                                                                         db: DB): DbResultT[Unit] =
    for {
      group          ← * <~ CustomerGroups.mustFindById404(groupId)
      _              ← * <~ group.mustNotBeOfType(Manual)
      currentMembers ← * <~ CustomerGroupMembers.findByGroupId(group.id).result
      dataIds = currentMembers.map(_.customerDataId).toSet
      currentMemberData ← * <~ CustomersData.findAllByIds(dataIds).result
      memberIds   = currentMemberData.map(_.accountId).toSet
      newIds      = payload.customers.toSet
      forCreation = newIds.diff(memberIds).toSeq
      forDeletion = memberIds.diff(newIds).toSeq
      _ ← * <~ forCreation.map { userId ⇒
           createGroupMember(userId, groupId)
         }
      _ ← * <~ forDeletion.map { userId ⇒
           deleteGroupMember(userId, groupId)
         }
    } yield ()

  def sync(groupId: Int,
           payload: CustomerGroupMemberSyncPayload)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      group          ← * <~ CustomerGroups.mustFindById404(groupId)
      _              ← * <~ group.mustBeOfType(Manual)
      currentMembers ← * <~ CustomerGroupMembers.findByGroupId(group.id).result
      dataIds = currentMembers.map(_.customerDataId).toSet
      currentMemberData ← * <~ CustomersData.findAllByIds(dataIds).result
      memberIds   = currentMemberData.map(_.accountId).toSet
      forCreation = payload.toAdd.toSet
      forDeletion = payload.toDelete.toSet
      _ ← * <~ failIf(
           !forCreation.intersect(forDeletion).isEmpty,
           CustomerGroupMemberPayloadContainsSameIdsInBothSections(groupId, forCreation, forDeletion))
      _ ← * <~ forCreation.diff(memberIds).toSeq.map { userId ⇒
           createGroupMember(userId, groupId)
         }
      _ ← * <~ forDeletion.intersect(memberIds).toSeq.map { userId ⇒
           deleteGroupMember(userId, groupId)
         }
    } yield ()

  def addCustomerToGroups(accountId: Int,
                          groupIds: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[CustomerResponse] =
    for {
      customer  ← * <~ Users.mustFindByAccountId(accountId)
      newGroups ← * <~ CustomerGroups.findAllByIds(groupIds.toSet).result
      check ← * <~ newGroups.map { group ⇒
               DbResultT.fromEither(group.mustBeOfType(Manual))
             }
      customerDatas ← * <~ CustomersData
                       .filter(_.accountId === accountId)
                       .withRegionsAndRank
                       .mustFindOneOr(NotFoundFailure404(CustomerData, accountId))
      (customerData, shipRegion, billRegion, rank) = customerDatas
      maxOrdersDate ← * <~ Orders.filter(_.accountId === accountId).map(_.placedAt).max.result
      totals        ← * <~ StoreCreditService.fetchTotalsForCustomer(accountId)
      phoneOverride ← * <~ doOrGood(customer.phoneNumber.isEmpty,
                                    CustomerManager.resolvePhoneNumber(accountId),
                                    None)
      groupMembership ← * <~ CustomerGroupMembers.findByCustomerDataId(customerData.id).result
      newGroupIds = newGroups.map(_.id).toSet
      groupIds    = groupMembership.map(_.groupId).toSet
      manualGroupsOfUser ← * <~ CustomerGroups.fildAllByIdsAndType(groupIds, Manual).result
      manualGroupIdsOfUser = manualGroupsOfUser.map(_.id).toSet
      forCreation          = newGroupIds.diff(manualGroupIdsOfUser).toSeq
      forDeletion          = manualGroupIdsOfUser.diff(newGroupIds).toSeq
      _ ← * <~ forCreation.map { groupId ⇒
           createGroupMember(accountId, groupId)
         }
      _ ← * <~ forDeletion.map { groupId ⇒
           deleteGroupMember(accountId, groupId)
         }
      dynamicGroupsOfUser ← * <~ CustomerGroups.fildAllByIdsAndType(groupIds, Dynamic).result
    } yield
      CustomerResponse.build(
        customer.copy(phoneNumber = customer.phoneNumber.orElse(phoneOverride)),
        customerData,
        shipRegion,
        billRegion,
        rank = rank,
        scTotals = totals,
        lastOrderDays = maxOrdersDate.map(DAYS.between(_, Instant.now)),
        groups = (dynamicGroupsOfUser ++ newGroups).map(CustomerGroupResponse.build _)
      )

  private def createGroupMember(userId: Int, groupId: Int)(implicit ec: EC,
                                                           db: DB): DbResultT[CustomerGroupMember] =
    for {
      customerData ← * <~ CustomersData.mustFindByAccountId(userId)
      group        ← * <~ CustomerGroups.mustFindById400(groupId)
      membership = CustomerGroupMember(customerDataId = customerData.id, groupId = groupId)
      result ← * <~ CustomerGroupMembers.create(membership)
      _ ← * <~ when(group.groupType == Manual,
                    CustomerGroups.update(group, group.copy(customersCount = group.customersCount + 1)).void)
    } yield result

  private def deleteGroupMember(userId: Int, groupId: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      customerData ← * <~ CustomersData.mustFindByAccountId(userId)
      group        ← * <~ CustomerGroups.mustFindById400(groupId)
      membership ← * <~ CustomerGroupMembers
                    .findByGroupIdAndCustomerDataId(customerData.id, groupId)
                    .mustFindOneOr(NotFoundFailure400(User, userId))
      _ ← * <~ CustomerGroupMembers
           .deleteById(membership.id, ().pure[DbResultT], userId ⇒ NotFoundFailure400(User, userId))
      _ ← * <~ when(group.groupType == Manual,
                    CustomerGroups.update(group, group.copy(customersCount = group.customersCount - 1)).void)
    } yield ()

  def isMemberOfAny(groupIds: Set[Int], customer: User)(implicit ec: EC, apis: Apis): DbResultT[Boolean] =
    for {
      customerGroups ← * <~ CustomerGroups.fildAllByIds(groupIds).result.dbresult
      results ← * <~ customerGroups.toStream
                 .traverse(isMember(_, customer)) // FIXME: no need to check all of them, but Scala is strict… @michalrus
    } yield results.exists(identity)

  def isMember(group: CustomerGroup, customer: User)(implicit ec: EC, apis: Apis): DbResultT[Boolean] =
    if (group.groupType == Manual) for {
      customerData ← * <~ CustomersData.mustFindByAccountId(customer.accountId)
      num ← * <~ CustomerGroupMembers
             .findByGroupIdAndCustomerDataId(customerDataId = customerData.id, groupId = group.id)
             .size
             .result
             .dbresult
    } yield (num > 0)
    else if (group.groupType == Dynamic)
      for {
        num ← (* <~ apis.elasticSearch.numResults(
               ElasticsearchApi.SearchView(SearchReference.customersSearchView),
               narrowDownWithUserId(customer.id)(group.elasticRequest))).failuresToWarnings(0) {
               case _ ⇒ true
             }
        // FIXME: make sure the warning bubbles up to the final response — monad stack order should be different, we don’t want to lose warnings when a Failure happens @michalrus
      } yield (num > 0)
    else false.pure[DbResultT]

  private def narrowDownWithUserId(userId: Int)(elasticRequest: Json): Json = {
    val term      = JObject(JField("term", JObject(JField("id", JInt(userId)))))
    val userQuery = JObject(JField("query", JObject(JField("bool", JObject(JField("must", term))))))

    JObject(
      JField("query",
             JObject(JField("bool", JObject(JField("filter", JArray(List(elasticRequest, userQuery))))))))
  }

}
