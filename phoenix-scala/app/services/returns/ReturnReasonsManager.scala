package services.returns

import models.returns._
import responses.ReturnResponse.Root
import slick.driver.PostgresDriver.api._
import utils.aliases.{DB, EC}
import utils.db.{*, DbResultT, _}

object ReturnReasonsManager {

  def reasonsList(implicit ec: EC, db: DB): DbResultT[Seq[ReturnReason]] =
    for {
      reasons ← * <~ ReturnReasons.result
    } yield reasons
}
