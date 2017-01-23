package services.customerGroups

import payloads.CustomerGroupPayloads.CustomerGroupMemberSyncPayload
import utils.aliases._
import utils.db.DbResultT

object GroupMemberManager {

  def sync(groupId: Int, payload: CustomerGroupMemberSyncPayload)(implicit ec: EC): DbResultT[Unit] = {
    DbResultT.unit
  }

}
