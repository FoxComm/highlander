package services.customerGroups

import utils.aliases._
import utils.db.DbResultT

object GroupMemberManager {

  def sync()(implicit ec: EC): DbResultT[Unit] = {
    DbResultT.unit
  }

}
