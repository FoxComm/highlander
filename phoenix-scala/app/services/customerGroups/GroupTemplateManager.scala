package services.customerGroups

import models.account.Scope
import models.customer._
import utils.aliases._
import utils.db._
import utils.db.ExPostgresDriver.api._

object GroupTemplateManager {

  def getAll()(implicit ec: EC, db: DB, au: AU): DbResultT[Seq[CustomerGroupTemplate]] =
    for {
      scope         ← * <~ Scope.current
      usedTemplates ← * <~ GroupTemplateInstances.findByScope(scope).map(_.groupTemplateId).result
      templates     ← * <~ CustomerGroupTemplates.result
    } yield templates.filter(t ⇒ !usedTemplates.contains(t.id))
}
