package services.customerGroups

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import failures.CustomerGroupFailures.CustomerGroupMemberPayloadContainsSameIdsInBothSections
import failures.{NotFoundFailure400, NotFoundFailure404}
import models.account.{User, Users}
import models.cord.Orders
import models.customer.CustomerGroup._
import models.customer.CustomersData.scope._
import models.customer._
import payloads.CustomerGroupPayloads._
import responses.CustomerResponse.{Root, build}
import responses.GroupResponses.CustomerGroupResponse
import services.StoreCreditService
import services.customers.CustomerManager
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

object GroupMemberManager {

  def sync(groupId: Int, payload: CustomerGroupMemberServiceSyncPayload)(implicit ec: EC,
                                                                         db: DB): DbResultT[Unit] =
    for {
      group          ← * <~ CustomerGroups.mustFindById404(groupId)
      _              ← * <~ group.mustNotBeOfType(Manual)
      currentMembers ← * <~ CustomerGroupMembers.findByGroupId(group.id).result
      dataIds = currentMembers.map(_.customerDataId).toSet
      currentMemberData ← * <~ CustomersData.findAllByIds(dataIds).result
      memberIds   = currentMemberData.map(_.userId).toSet
      newIds      = payload.customers.toSet
      forCreation = newIds.diff(memberIds).toSeq
      forDeletion = memberIds.diff(newIds).toSeq
      _ ← * <~ forCreation.map { userId ⇒
           createGroupMember(userId, groupId)
         }
      _ ← * <~ forDeletion.map { userId ⇒
           deleteGroupMember(userId, groupId)
         }
    } yield {}

  def sync(groupId: Int, payload: CustomerGroupMemberSyncPayload)(implicit ec: EC,
                                                                  db: DB,
                                                                  ac: AC): DbResultT[Unit] =
    for {
      group          ← * <~ CustomerGroups.mustFindById404(groupId)
      _              ← * <~ group.mustBeOfType(Manual)
      currentMembers ← * <~ CustomerGroupMembers.findByGroupId(group.id).result
      dataIds = currentMembers.map(_.customerDataId).toSet
      currentMemberData ← * <~ CustomersData.findAllByIds(dataIds).result
      memberIds   = currentMemberData.map(_.userId).toSet
      forCreation = payload.toAdd.toSet
      forDeletion = payload.toDelete.toSet
      _ ← * <~ failIf(!forCreation.intersect(forDeletion).isEmpty,
                      CustomerGroupMemberPayloadContainsSameIdsInBothSections(groupId,
                                                                              forCreation,
                                                                              forDeletion))
      _ ← * <~ forCreation.diff(memberIds).toSeq.map { userId ⇒
           createGroupMember(userId, groupId)
         }
      _ ← * <~ forDeletion.intersect(memberIds).toSeq.map { userId ⇒
           deleteGroupMember(userId, groupId)
         }
    } yield {}

  def addCustomerToGroups(accountId: Int,
                          groupIds: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      customer  ← * <~ Users.mustFindByAccountId(accountId)
      newGroups ← * <~ CustomerGroups.findAllByIds(groupIds.toSet).result
      check ← * <~ newGroups.map { group ⇒
               DbResultT.fromXor(group.mustBeOfType(Manual))
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
      build(customer.copy(phoneNumber = customer.phoneNumber.orElse(phoneOverride)),
            customerData,
            shipRegion,
            billRegion,
            rank = rank,
            scTotals = totals,
            lastOrderDays = maxOrdersDate.map(DAYS.between(_, Instant.now)),
            groups = (dynamicGroupsOfUser ++ newGroups).map(CustomerGroupResponse.build _))

  private def createGroupMember(userId: Int, groupId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[CustomerGroupMember] =
    for {
      customerData ← * <~ CustomersData.mustFindByAccountId(userId)
      group        ← * <~ CustomerGroups.mustFindById400(groupId)
      membership = CustomerGroupMember(customerDataId = customerData.id, groupId = groupId)
      result ← * <~ CustomerGroupMembers.create(membership)
      _ ← * <~ doOrMeh(
             group.groupType == Manual,
             CustomerGroups.update(group, group.copy(customersCount = group.customersCount + 1)))
    } yield result

  private def deleteGroupMember(userId: Int, groupId: Int)(implicit ec: EC,
                                                           db: DB): DbResultT[Unit] =
    for {
      customerData ← * <~ CustomersData.mustFindByAccountId(userId)
      group        ← * <~ CustomerGroups.mustFindById400(groupId)
      membership ← * <~ CustomerGroupMembers
                    .findByGroupIdAndCustomerDataId(customerData.id, groupId)
                    .mustFindOneOr(NotFoundFailure400(User, userId))
      _ ← * <~ CustomerGroupMembers
           .deleteById(membership.id, DbResultT.unit, userId ⇒ NotFoundFailure400(User, userId))
      _ ← * <~ doOrMeh(
             group.groupType == Manual,
             CustomerGroups.update(group, group.copy(customersCount = group.customersCount - 1)))
    } yield {}

}
