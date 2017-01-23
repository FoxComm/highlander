package services.customerGroups

import models.account.{User, Users}
import models.customer._
import payloads.CustomerGroupPayloads.CustomerGroupMemberSyncPayload
import utils.aliases._
import utils.db._
import utils.db.ExPostgresDriver.api._
import cats.implicits._
import failures.NotFoundFailure400

object GroupMemberManager {

  def sync(groupId: Int, payload: CustomerGroupMemberSyncPayload)(implicit ec: EC,
                                                                  db: DB): DbResultT[Unit] =
    for {
      group          ← * <~ CustomerDynamicGroups.mustFindById404(groupId)
      currentMembers ← * <~ CustomerGroupMembers.findByGroupId(group.id).result
      dataIds = currentMembers.map(_.customerDataId).toSet
      currentMemberData ← * <~ CustomersData.findAllByIds(dataIds).result
      memberIds   = currentMemberData.map(_.userId).toSet
      newIds      = payload.customers.toSet
      forCreation = newIds.diff(memberIds)
      forDeletion = memberIds.diff(newIds)
      _ ← * <~ forCreation.map { userId ⇒
            createGroupMember(userId, groupId)
          }
      _ ← * <~ forDeletion.map { userId ⇒
            deleteGroupMember(userId, groupId)
          }
    } yield {}

  private def createGroupMember(userId: Int, groupId: Int)(implicit ec: EC,
                                                           db: DB): DbResultT[Unit] =
    for {
      customerData ← * <~ CustomersData.mustFindByAccountId(userId)
      membership = CustomerGroupMember(customerDataId = customerData.id, groupId = groupId)
      _ ← * <~ CustomerGroupMembers.create(membership)
    } yield {}

  private def deleteGroupMember(userId: Int, groupId: Int)(implicit ec: EC,
                                                           db: DB): DbResultT[Unit] =
    for {
      customerData ← * <~ CustomersData.mustFindByAccountId(userId)
      membership ← * <~ CustomerGroupMembers
                     .findByGroupIdAndCustomerDataId(customerData.id, groupId)
                     .mustFindOneOr(NotFoundFailure400(User, userId))
      _ ← * <~ CustomerGroupMembers
            .deleteById(membership.id, DbResultT.unit, userId ⇒ NotFoundFailure400(User, userId))
    } yield {}

}
