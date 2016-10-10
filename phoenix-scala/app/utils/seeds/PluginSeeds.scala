package utils.seeds

import utils.aliases._
import utils.db.DbResultT

trait PluginSeeds {

  def createAvalaraSettings()(implicit db: DB, ac: AC, ec: EC): DbResultT[Unit] = {
    DbResultT.unit
  }

}
