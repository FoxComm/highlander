package services.customerGroups

import models.customer.{CustomerGroupTemplate, CustomerGroupTemplates}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.{*, DbResultT}

object GroupTemplateManager {

  def getAll()(implicit ec: EC, db: DB, au: AU): DbResultT[Seq[CustomerGroupTemplate]] =
    for {
      templates ← * <~ CustomerGroupTemplates.result.dbresult
    } yield templates

}
